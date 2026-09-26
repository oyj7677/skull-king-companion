# v1 Active Game Scoreboard

## Problem Statement

스컬킹 게임 중에는 최대 8명의 입찰, 실제 획득 트릭, 카드 보너스, 누적 점수를 빠르고 정확하게 기록해야 한다. 현재 앱은 한 화면의 임시 메모리에만 상태를 두므로 Android 생명주기에 따라 기록을 잃을 수 있고, 게임 설정·라운드 수정·완료 결과 복구를 지원하지 않는다.

## Solution

태블릿 가로 화면을 우선하는 단일 Active Game 점수판을 제공한다. 사용자는 게임을 시작하기 전에 2~8명의 Player, 총 Round 수(기본 10), Rule Set을 설정한다. 각 Round에서 모든 Player의 입찰과 획득 트릭을 한 화면에서 입력하고, 입찰 성공 시에만 Round Bonus를 직접 추가한다. Game, Player, Round 기록은 기기 로컬에 영구 저장되며, 완료된 게임은 사용자가 새 게임을 시작할 때까지 결과 화면으로 보관한다.

## User Stories

1. As a 게임 진행자, I want to 시작 전 Player를 2~8명으로 추가하고 이름을 편집할 수 있도록, so that 실제 참가자에 맞는 점수판을 만들 수 있다.
2. As a 게임 진행자, I want to 총 Round 수를 기본값 10에서 시작 전에 변경할 수 있도록, so that 우리 모임의 게임 길이에 맞출 수 있다.
3. As a 게임 진행자, I want to 시작 전에 Rule Set을 확인하도록, so that 점수 계산의 전제가 게임 내내 일관되게 유지된다.
4. As a 게임 진행자, I want to 첫 Round를 시작하면 Player 구성이 잠기도록, so that 기록의 주체와 누적 순위가 바뀌지 않는다.
5. As a Player, I want to 가로 태블릿 한 화면에서 모든 Player의 행을 볼 수 있도록, so that 입력 누락 없이 라운드 결과를 함께 확인할 수 있다.
6. As a 게임 진행자, I want to 각 Player의 입찰과 획득 트릭을 입력하도록, so that 기본 라운드 점수가 계산된다.
7. As a 게임 진행자, I want to 입찰 성공 Player에게만 Round Bonus를 직접 입력하도록, so that 판본·확장·하우스 룰의 카드 보너스를 빠르게 반영할 수 있다.
8. As a 게임 진행자, I want to Round를 저장하기 전에 모든 Player의 기록 완료 여부를 확인하도록, so that 불완전한 라운드를 확정하지 않는다.
9. As a Player, I want to 각 Round의 기본 점수, Round Bonus, 합계와 누적 점수를 확인하도록, so that 점수의 근거를 이해할 수 있다.
10. As a 게임 진행자, I want to 저장한 Round를 다시 편집하도록, so that 기록 실수를 바로잡고 이후 누적 점수와 순위를 다시 계산할 수 있다.
11. As a 게임 진행자, I want to 화면 회전·멀티 윈도우·백그라운드 종료 뒤에도 Active Game을 복구하도록, so that 진행 중인 게임 기록을 잃지 않는다.
12. As a 게임 진행자, I want to 마지막 Round 뒤 Completed Game의 순위와 점수를 계속 보도록, so that 테이블에서 최종 결과를 다시 확인할 수 있다.
13. As a 게임 진행자, I want to 새 게임 시작을 명시적으로 확인하도록, so that 기존 Completed Game을 실수로 지우지 않는다.
14. As a 게임 진행자, I want to 앱을 다시 열면 진행 중인 게임 또는 완료 결과로 돌아가도록, so that 매번 게임을 다시 설정하지 않아도 된다.

## Implementation Decisions

- 단일 Android 앱 모듈은 유지하되, domain, data, presentation 책임을 명확히 분리한다.
- domain은 Game, Player, Round, Rule Set, Round Bonus와 점수 계산을 소유한다. UI와 저장 기술에 의존하지 않는다.
- `ScoreCalculator`는 입찰, 획득 트릭, Round Bonus, Round 번호를 받아 기본 점수·보너스·총점을 명시하는 순수 규칙 모듈이다.
- `ActiveGameRepository`는 Active Game을 관찰하고 생성·수정·라운드 확정·완료·교체하는 유일한 데이터 인터페이스다.
- Room 로컬 데이터베이스는 Game, Player, Round 기록의 단일 원본이다. 한 번의 Round 저장 또는 수정은 관련 기록과 누적 결과를 일관되게 갱신한다.
- ViewModel은 Repository의 데이터를 읽기 전용 UI State로 변환하고 UI 이벤트를 처리한다. Compose UI는 UI State를 렌더링하고 이벤트만 전달한다.
- SavedStateHandle은 저장 전 입력값, 선택된 편집 대상 같은 짧은 화면 상태에만 사용한다. Active Game의 영속 데이터는 Room에 보관한다.
- 태블릿 가로 화면은 Player, 입찰, 획득, Round Bonus, 라운드 점수 열을 표시한다. 2~8개의 Player 행이 동시에 보여야 한다.
- 입찰 성공 여부는 입찰과 획득 트릭이 같은지로 판단한다. 불일치하면 Round Bonus는 0점이며 입력하지 않는다.
- 기본 Round Schedule은 1~10 Round다. Game 시작 시 총 Round 수를 설정할 수 있고, 시작 후에는 바꿀 수 없다.
- Completed Game은 결과 화면을 유지한다. 새 게임 시작은 확인 후 기존 Active Game을 대체한다.

## Testing Decisions

- 좋은 테스트는 구현 세부가 아니라 합의된 Interface에서 관찰되는 동작을 검증한다. 기대값은 규칙서의 계산 예시나 명세의 구체적 값처럼 독립된 근거에서 가져온다.
- `ScoreCalculator` seam은 0 입찰 성공·실패, 양수 입찰 성공·실패, Round Bonus 포함, 실패 시 보너스 제거, 수정 뒤 점수 재계산을 단위 테스트한다.
- `ActiveGameRepository` seam은 Game 생성, Player 구성 잠금, Round 저장 완전성, Round 수정, 단일 Active Game 복구, Completed Game 유지, 명시적 교체를 영속성 통합 테스트한다.
- ViewModel은 Repository Interface를 통해 점수판 UI State, 저장 가능 여부, 수정 후 누적·순위 변화를 테스트한다.
- Compose UI 테스트는 태블릿 점수판의 2명·8명 표시, 미완성 Round 저장 차단, 입찰 성공 시에만 Round Bonus 입력 표시를 검증한다.
- 기존의 단순 `ScoreCalculator` 단위 테스트는 유지하되, 위의 외부 동작을 표현하도록 확장한다.

## Out of Scope

- 여러 과거 Game의 목록·검색·보관·복원
- 클라우드 동기화, 계정, 여러 기기 공유
- 개별 카드와 매 Trick의 진행을 입력해 카드 보너스를 자동 판정하는 기능
- 게임 중 Player의 추가·삭제·이름 변경
- Game 시작 후 Rule Set 또는 총 Round 수 변경
- 고급 해적 능력, 확장 카드 효과, 사용자 정의 카드 효과의 자동 계산
- 휴대폰 우선 레이아웃과 세로 화면 최적화

## Further Notes

- 카드 보너스는 v1에서 Round Bonus 총점으로 직접 입력한다. 필요한 경우 메모는 둘 수 있지만, 카드별 원인을 구조화하지 않는다.
- 공식 보너스 규칙은 판본 차이가 있을 수 있으므로, v1의 자동 계산은 기본 입찰 점수에 한정하고 카드 보너스는 수동 입력으로 둔다.
- 용어는 `CONTEXT.md`와 ADR 0001~0008을 따른다.
