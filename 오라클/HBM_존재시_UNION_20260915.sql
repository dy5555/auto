-- 첫 번째 SELECT 결과에 CODE = 'HBM'이 존재할 경우에만 두 번째 쿼리 UNION

SELECT A.CODE
     , A.NAME
  FROM TABLE_A A
 WHERE A.USE_YN = 'Y'

UNION

SELECT B.CODE
     , B.NAME
  FROM TABLE_B B
 WHERE EXISTS (
       SELECT 1
         FROM TABLE_A C
        WHERE C.USE_YN = 'Y'
          AND C.CODE = 'HBM'
       )
;
