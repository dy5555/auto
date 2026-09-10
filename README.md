# auto

Java 8 + Swing 기반 업무용 데스크톱 도구입니다.

## 현재 기능

- 개발 DB / 운영 DB 탭 분리
- 조회 시작일 / 종료일 지정
- Oracle DB 프로시저 실행 현황 조회
- DB 조회 중 / 완료 / 실패 상태 표시
- 전체 / 정상 / 진행중 / 오류 건수 요약
- 결과 JTable 그리드 표시
- 정상(초록), 진행중(노랑), 오류(빨강) 색상 구분
- 10개 기본 컬럼 제공
- 에러설명 컬럼 넓게 표시
- 행 더블클릭 시 에러설명 포함 전체 상세 팝업 표시
- DB 조회는 SwingWorker로 실행하여 조회 중에도 UI 멈춤 최소화

## 기본 결과 컬럼

1. 상태
2. 프로시저명
3. 작업ID
4. 시작시간
5. 종료시간
6. 수행시간(초)
7. 대상일자
8. 처리건수
9. 에러코드
10. 에러설명

실제 DB 컬럼명은 `config/app.properties`의 SQL alias 부분만 수정하면 됩니다.

## 실행 준비

1. Java 8 JDK 설치
2. Oracle JDBC 드라이버 `ojdbc8.jar`를 `lib/ojdbc8.jar`로 복사
3. `config/app.properties`에 개발/운영 DB 접속정보 입력
4. `query.procedureStatus`를 실제 테이블에 맞게 수정
5. `build.bat` 실행
6. `run.bat` 실행

## 보안 주의

실제 운영 DB 비밀번호는 GitHub에 커밋하지 않는 것을 권장합니다. 현재 `app.properties` 값은 예시값입니다.
