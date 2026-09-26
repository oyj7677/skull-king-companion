# Active Game은 기기에 영구 저장한다

게임의 점수 기록은 Activity나 Compose 메모리에 두지 않고 기기 로컬 데이터베이스에 보관한다. 화면 회전, 멀티 윈도우, 백그라운드 종료, 프로세스 종료 뒤에도 Active Game을 동일한 상태로 복구하기 위해서다.

## Considered Options

- Compose 상태와 ViewModel만 사용: 구성 변경에는 대응하지만, 프로세스 종료 뒤의 게임 기록을 보장하지 못한다.
- 수동 파일 저장: 작은 데이터에는 가능하지만, Player·Round·점수 기록의 원자적 변경과 조회를 안전하게 다루기 어렵다.

## Consequences

- Game, Player, Round 기록은 Room 로컬 데이터베이스의 단일 원본으로 둔다.
- 화면은 ViewModel이 데이터베이스 기록으로부터 만든 읽기 전용 UI 상태를 표시한다.
- 저장하지 않은 숫자 입력처럼 짧은 화면 상태만 SavedStateHandle로 복구한다.
