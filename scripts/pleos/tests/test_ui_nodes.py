from copy import deepcopy
from pathlib import Path
import sys
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).parents[1]))
from ui_nodes import labeled_toggle

EVIDENCE = Path(__file__).parents[3] / 'docs/evidence/android-ui-screens'


class ActualComposeToggleTest(unittest.TestCase):
    def nodes(self, name):
        return list(ET.parse(EVIDENCE / (name + '.xml')).getroot().iter('node'))

    def test_actual_compose_source_switch_has_no_switch_class(self):
        nodes = self.nodes('11-test-inputs')
        self.assertFalse(any('Switch' in n.attrib.get('class', '') for n in nodes))
        for label in ['차량 신호 테스트 입력', '내비 모의 요청', '알림 서버 사용']:
            node = labeled_toggle(nodes, label)
            self.assertEqual('true', node.attrib['checkable'])
            self.assertEqual('android.view.View', node.attrib['class'])

    def test_profile_switch_retains_actual_disabled_state_while_driving(self):
        nodes = self.nodes('12-driving-lock')
        for label in ['왼쪽 카시트 문 여유', '오른쪽 카시트 문 여유', '유모차 · 무단차 필요']:
            self.assertEqual('false', labeled_toggle(nodes, label).attrib['enabled'])

    def test_ambiguous_and_missing_controls_are_rejected(self):
        nodes = self.nodes('11-test-inputs')
        with self.assertRaises(RuntimeError):
            labeled_toggle(nodes, 'not present')
        nodes.append(deepcopy(labeled_toggle(nodes, '내비 모의 요청')))
        with self.assertRaises(RuntimeError):
            labeled_toggle(nodes, '내비 모의 요청')


if __name__ == '__main__':
    unittest.main()
