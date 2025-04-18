-- 사용자 생성
CREATE USER secuiot WITH PASSWORD 'secuiot1q2w';

-- 리눅스에서 사용
CREATE TABLESPACE ts_temp OWNER secuiot location '/var/lib/postgresql/data/ts_temp';

------------------------------------------------------------------------------------
--사용자 default_tablespace 설정
------------------------------------------------------------------------------------
ALTER ROLE secuiot SET DEFAULT_TABLESPACE TO ts_temp;

------------------------------------------------------------------------------------
--database 생성 및 default_tablespace 설정
------------------------------------------------------------------------------------
CREATE DATABASE secuiot OWNER secuiot;

ALTER DATABASE secuiot SET DEFAULT_TABLESPACE to ts_temp;


--connect secuiot secuiot
--------------------------------------------------------------------------------------------------------------
-- DB를 secuiot로 재접속한다
-- 0.실행 : secuiot(DB) secuiot(sechema) secuiot(계정)
--------------------------------------------------------------------------------------------------------------
CREATE SCHEMA secuiot AUTHORIZATION secuiot;
