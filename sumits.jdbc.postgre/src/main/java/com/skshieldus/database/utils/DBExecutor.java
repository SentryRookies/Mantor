package com.skshieldus.database.utils;

import org.apache.ibatis.session.SqlSession;

import java.util.List;

public interface DBExecutor {

    static <E> List<E> selectList(SqlSession session, String statement, Object parameter, boolean isSessionClosed) {

        try {
            return session.selectList(statement, parameter);
        } finally {
            if(isSessionClosed){
                session.close();
            }
        }

    }
    static <E> List<E> selectList(SqlSession session, String statement, Object parameter) {
        return selectList(session, statement, parameter, true);
    }


    static <T> T selectOne(SqlSession session, String statement, Object parameter, boolean isSessionClosed) {
        try {
            return session.selectOne(statement, parameter);
        } finally {
            if(isSessionClosed){
                session.close();
            }
        }
    }
    static <T> T selectOne(SqlSession session, String statement, Object parameter) {
        return selectOne(session, statement, parameter, true);
    }

    static int insert(SqlSession session, String statement, Object parameter) {
        return insert(session, statement, parameter, true);
    }

    static int insert(SqlSession session, String statement, Object parameter, boolean isSessionClosed) {
        try {
            return session.insert(statement, parameter);
        } finally {
            if(isSessionClosed){
                session.close();
            }
        }
    }

    static int update(SqlSession session, String statement, Object parameter) {
        return update(session, statement, parameter, true);
    }

    static int update(SqlSession session, String statement, Object parameter, boolean isSessionClosed) {
        try {
            return session.update(statement, parameter);
        } finally {
            if(isSessionClosed){
                session.close();
            }
        }
    }

    static int delete(SqlSession session, String statement, Object parameter) {
        return delete(session, statement, parameter, true);
    }

    static int delete(SqlSession session, String statement, Object parameter, boolean isSessionClosed) {
        try {
            return session.delete(statement, parameter);
        } finally {
            if(isSessionClosed){
                session.close();
            }
        }
    }

}
