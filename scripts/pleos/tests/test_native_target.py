from copy import deepcopy
import json
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]))
from x11_context import input_target_known


class ActualNativeTargetTest(unittest.TestCase):
    def context(self):
        return json.loads((Path(__file__).parents[3]/'docs/evidence/pleos-native-target/context-before-input.json').read_text())
    def test_actual_native_emulator_target_overrides_false_zero_from_xdotool(self):
        context=self.context()
        self.assertEqual(0,context['pointer']['WINDOW'])
        self.assertEqual(4194311,context['native_state']['direct_pointer']['child'])
        self.assertTrue(input_target_known(context))
    def test_incomplete_hidden_wrong_class_or_wrong_position_are_not_targets(self):
        original=self.context()
        changes=[lambda c:c['native_state']['windows'][0].update(map_state=0),
                 lambda c:c['native_state']['windows'][0].update(point_in_input_region=False),
                 lambda c:c['native_state']['windows'][0].update(input_regions_complete=False),
                 lambda c:c['native_state']['direct_pointer'].update(x=100),
                 lambda c:c['native_state']['direct_pointer'].update(child=0),
                 lambda c:c['native_state'].update(x11_errors=[{'error':3}]),
                 lambda c:c['window_inventory']['windows'][3].update(classes=['unknown'])]
        for change in changes:
            context=deepcopy(original);change(context)
            self.assertFalse(input_target_known(context))


if __name__=='__main__':unittest.main()
