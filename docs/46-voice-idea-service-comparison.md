# 음성 아이디어6개 · 실제 제공 서비스 대조

2026-11-14 UTC. 공식 제품 도움말·제조사 배포공지·개발사 앱스토어 설명을 기준으로 조사했다. 직접 가입/차량사용/유료계약 시험은 하지 않았다. '제공 확인'은 공개 사용법 또는 배포정보 확인이며 실사용 규모/한국·Pleos 동일 제공까지 보장하지 않는다. 특허/전시만으로 운영 중 판정하지 않았다.

| 후보 | 실제 제공 근거 | 중복/미확인 판단 |
| --- | --- | --- |
| 1 음성 할 일/도착 후 알림 | Apple Siri+Reminders는 음성으로 집 도착/출발 때 알릴 일을 등록하는 공식 예시와 사용법 제공 | 핵심 경험 높은 중복. 차량 목적지/정차에 연결한 UI만으로 신규 핵심으로 보기 어려움 |
| 2 정비 증상 음성기록/차량상태 결합 | Tesla 앱은 사용자 증상설명→진단/정비예약·이력 조회 제공 | 부분 중복. 음성 발생시점의 Pleos 상태를 자동결합해 운전자용 정비보고서로 완성하는 동일전체서비스는 이번에 확인 못함. 오래된Tesla PDF의bug report를 현재정비접수통합으로 오해하지 않음 |
| 3 다음 운전자 인계메모 | Co-oto는 개인차량 공유/다음운전자 주차메모, vAutoMove Driver는 차량인계점검·음성점검메모전사·내비연결 제공 | 핵심 조합의 상당 부분 기존. Co-oto음성예약이 음성인계라는뜻은 아님. vAutoMove는 업무계정필수/스토어설명기준이며사용규모미확인. 가족교대+실차SDK차이+관련음성메모자동매칭의동일전체제품은미확인 |
| 4 방문업무 음성보고 | Salesforce Field Service Voice to Record Edit: 음성으로 업무레코드필드입력. Summer25이후앱과별도라이선스/권한조건을명시 | 높은중복.별도Voice to Form Beta를일반제공완료로합산하지않음.방문후요약/다음내비까지모든흐름동일하다고단정하지않음 |
| 5 목적/조건을말해장소추천/내비연결 | Google Maps Gemini: 경로상검색/장소질문/결과정제/경유추가사용법. Gleo는2026-10-30현대공식공지에서그랜저Pleos OTA로음성맥락검색/경로상장소추천/목적지설정배포설명 | 높은중복.특히Pleos자체경쟁기능.모든휠체어/유모차동선조건까지제공한다고확인한것은아님.구글기능은지역/계정/언어조건과배포범위차이있음 |
| 6 초보운전자 음성회고/다음연습목표 | Journey는유료음성노트녹음/일기첨부제공.운전학습회고제품검색도했으나개발미리보기를운영서비스증거로채택하지않음 | 음성일기구성요소는기존.정차후초보운전회고→다음연습목표의동일통합서비스는이번에미확인.그것만으로독창성/사업성입증은아님 |

## 공식 근거

1. [Apple Reminders 사용법](https://support.apple.com/en-us/102484): Siri추가와위치기반알림.영문Siri상세페이지는429였지만일반지원문서본문에서두기능/예시를독립확인했다.
2. [Tesla 서비스 예약](https://www.tesla.com/support/service-visits): 증상설명·진단·예약/견적/이력.음성자동구조화나차량스냅샷보고서동일제품근거는아니다.
3. [Co-oto App Store](https://apps.apple.com/us/app/co-oto-private-car-sharing/id6466618867): 가족/개인공유및버전2.8의다음사용자주차메모,3.0의음성예약은다른기능이다.
4. [vAutoMove Driver Google Play](https://play.google.com/store/apps/details?id=com.vautomove.driver): 개발자배포설명에서음성점검메모전사/차량점검/내비연결.업무제공계정필수이며평가당시다운로드표시는1+여서널리쓰이는서비스로표현하지않는다.
5. [Salesforce 음성레코드입력 설정](https://help.salesforce.com/s/articleView?id=service.mfs_voice_to_record_edit_setup.htm&language=en_US&type=5): 공식본문의제품라이선스/앱버전/필드입력조건확인.자동조사요약의Beta자료와분리했다.
6. [Google Maps Gemini 도움말](https://support.google.com/maps/answer/6041199?hl=EN): 현재제공기능사용법. [Ask Maps배포공지](https://blog.google/products-and-platforms/products/maps/ask-maps-immersive-navigation/)의시작범위는미국/인도,한국전체제공으로바꾸지않는다.
7. [현대 Gleo OTA 2026-10-30](https://www.hyundaimotorgroup.com/ko/news/42dot-gleo-ai-first-ota-in-car-ai-experience-upgrade): 더뉴그랜저고객대상배포공지.단순미래기술발표와구분했다.
8. [Journey 음성노트](https://support.journey.cloud/en/categories/writing-editing/articles/record-audio-notes): 모바일/데스크톱음성노트유료제공,웹미지원.
9. [Movcar 인계가이드](https://blog.movcar.app/help/mobile-app/driver-handover/): 운전자교대/주행거리/상태/메모/PDF.가이드자체Beta article주의표시가있어세부UI를무조건실사용검증으로보지않는다.음성메모근거는위vAutoMove와구분한다.

## 권고 · 채택 결정은 아님

음성입력자체는차별점이약하다.1/4/5를그대로신규핵심으로내세우기는추천하지않는다.2/3을발전시키려면'공식SDK로확인한이전/현재상태차이→해당변화관련발화연결→다음사용자의확인/후속처리'같은좁은사용완결성이필요하다.6은동일서비스를못찾았더라도반복사용가치검증이먼저다.이번조사가전세계에같은제품이없음을증명하지않으며기존서비스존재자체가출품불가를뜻하지않는다.

도착자리음성주차기억안은사용자요청대로보관중이다.새아이디어채택/앱구현/외부문의·계정가입·설치·개인정보입력은하지않았다.
