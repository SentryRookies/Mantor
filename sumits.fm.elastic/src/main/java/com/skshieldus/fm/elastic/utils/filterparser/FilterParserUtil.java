package com.skshieldus.fm.elastic.utils.filterparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skshieldus.fm.elastic.utils.filterparser.domain.BooleanNode;
import com.skshieldus.fm.elastic.utils.filterparser.domain.FilterNode;
import com.skshieldus.fm.elastic.utils.filterparser.domain.Node;
import com.skshieldus.fm.elastic.utils.filterparser.enums.FilterOperator;
import com.skshieldus.utils.exception.BadRequestException;

import java.util.*;

public class FilterParserUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> buildQuery(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }

        if (!isInfix(filters)) {
            throw new BadRequestException("Invalid expression");
        }

        List<String> postfix = toPostfix(filters);

        Node ast = toAST(postfix);
        return ast.toEsQuery();
    }

    /**
     * 필터가 infix 형태인지 확인
     * postfix 형태로 올 경우 변환과정에서 검증되지 않음
     */
    private static boolean isInfix(List<String> filters) {
        boolean prevToken = true; // true = operator, false = filter
        int count = 0;

        for (String token: filters) {

            // 괄호가 있다면 infix 형태로 판단
            if (FilterOperator.LEFT_PARENTHESIS.equals(token) || FilterOperator.RIGHT_PARENTHESIS.equals(token)) {
                return true;
            }

            count++;

            if (isOperator(token)) {
                // 연산자 일 때 이전 토큰이 연산자면 false
                if (prevToken) {
                    return false;
                }
                prevToken = true;
            }
            else {
                // 필터 일 때 이전 토큰이 필터면 false
                if (!prevToken) {
                    return false;
                }
                prevToken = false;
            }
        }

        // 마지막 토큰이 연산자 혹은 전체 토큰 개수가 짝수일 경우 false
        if (prevToken || count % 2 == 0) {
            return false;
        }

        return true;
    }

    /**
     * infix -> postfix 형태로 변환
     */
    private static List<String> toPostfix(List<String> filters) {
        List<String> result = new ArrayList<>();
        Deque<String> opStack = new ArrayDeque<>();

        for (String filter: filters) {

            if (FilterOperator.LEFT_PARENTHESIS.equals(filter)) {
                opStack.push(filter);
            }
            else if (FilterOperator.RIGHT_PARENTHESIS.equals(filter)) {

                // 왼쪽 괄호가 나올 때까지 pop
                while (!opStack.isEmpty() && !FilterOperator.LEFT_PARENTHESIS.equals(opStack.peek())) {
                    result.add(opStack.pop());
                }
                if (!opStack.isEmpty() && FilterOperator.LEFT_PARENTHESIS.equals(opStack.peek())) {
                    opStack.pop();
                }
                // 괄호가 제대로 닫히지 않은 경우
                else {
                    throw new BadRequestException("Invalid expression");
                }
            }
            else if (isOperator(filter)) {
                while (!opStack.isEmpty() && (isOperator(opStack.peek()))
                        && precedence(filter) <= precedence(opStack.peek())) {
                    result.add(opStack.pop());
                }
                opStack.push(filter);
            }
            else {
                result.add(filter);
            }
        }

        while (!opStack.isEmpty()) {
            result.add(opStack.pop());
        }

        return result;
    }

    /**
     * postfix -> AST 형태로 변환
     */
    private static Node toAST(List<String> postfix) {
        Deque<Node> stack = new ArrayDeque<>();

        try {
            for (String filter: postfix) {
                if (isOperator(filter)) {
                    Node right = stack.pop();
                    Node left = stack.pop();
                    stack.push(new BooleanNode(filter, left, right));
                }
                else {
                    FilterNode filterNode = objectMapper.readValue(filter, FilterNode.class);
                    stack.push(filterNode);
                }
            }
        } catch (Exception e) { // 연산자 있는데 필터 노드 양 측에 없을 경우 (stack.pop() 실패) + 필터 노드가 아닌 경우 (readValue 실패)
            throw new BadRequestException("Invalid expression");
        }

        if (stack.size() != 1) { // 올바르지 않은 필터
            throw new BadRequestException("Invalid expression");
        }

        return stack.pop();
    }

    private static boolean isOperator(String filter) {
        return FilterOperator.AND.equals(filter)
                || FilterOperator.OR.equals(filter);
    }

    private static int precedence(String operator) {
        if (FilterOperator.AND.equals(operator)) {
            return 2;
        }
        else if (FilterOperator.OR.equals(operator)) {
            return 1;
        }
        return 0;
    }
}
