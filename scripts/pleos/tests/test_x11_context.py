from pathlib import Path
import subprocess
import sys
from types import SimpleNamespace
import unittest
sys.path.insert(0, str(Path(__file__).parents[1]))
from x11_context import observe_pointer, window_inventory, input_target_known


class X11ContextTest(unittest.TestCase):
    def test_readonly_records_actual_pointer_target_focus_and_geometry(self):
        calls = []
        def run(args, **kwargs):
            calls.append(args)
            output = {'getmouselocation': 'X=2403\nY=351\nSCREEN=0\nWINDOW=42',
                      'getdisplaygeometry': '3000 2000', 'getwindowfocus': '43'}
            value = output.get(args[1], '')
            if args[0] == 'xprop': value = 'WM_CLASS(STRING) = "emulator", "Emulator"'
            if args[0] == 'xwininfo': value = 'xwininfo: Window id:42 "title not retained"\n Absolute upper-left X: 100\n Absolute upper-left Y: 100\n Width: 2400\n Height: 1350\n Map State: IsViewable'
            return SimpleNamespace(returncode=0, stdout=value, stderr='')
        result = observe_pointer({}, (2403, 351), run)
        self.assertTrue(result['pointer_coordinates_match'])
        self.assertEqual(43, result['focus_window'])
        self.assertEqual({'x':100,'y':100,'width':2400,'height':1350}, result['target_geometry'])
        self.assertNotIn('title not retained', str(result))
        self.assertFalse(any('click' in arg or 'mousedown' in arg for call in calls for arg in call))

    def test_missing_target_and_clamped_coordinates_are_not_positive_evidence(self):
        def run(args, **kwargs):
            return SimpleNamespace(returncode=0, stdout='X=2999\nY=351\nSCREEN=0\nWINDOW=0' if args[1]=='getmouselocation' else '', stderr='')
        result = observe_pointer({}, (3403,351), run)
        self.assertFalse(result['pointer_coordinates_match'])
        self.assertIsNone(result['focus_window'])
        self.assertNotIn('target_class', result)

    def test_missing_tool_and_failure_preserve_return_codes(self):
        def run(args, **kwargs):
            if args[1] == 'getwindowfocus': raise FileNotFoundError('unavailable')
            return SimpleNamespace(returncode=1,stdout='',stderr='display unavailable')
        result = observe_pointer({}, (1,2), run)
        self.assertEqual(1,result['checks']['pointer']['returncode'])
        self.assertIsNone(result['checks']['focus']['returncode'])
        self.assertFalse(result['pointer_coordinates_match'])

    def test_inventory_omits_titles_and_unknown_target_cannot_emit_input(self):
        raw = 'xwininfo: Window id: 0x21f (the root window)\n 0x40000b "private title omitted" ("qemu" "Emulator")  2400x1350+100+100  +100+100'
        result = window_inventory(raw)
        self.assertEqual(543, result['root_id'])
        self.assertEqual(['qemu','Emulator'],result['windows'][0]['classes'])
        self.assertNotIn('private title',str(result))
        self.assertFalse(input_target_known({'pointer_coordinates_match':True,'pointer':{'WINDOW':0}}))
        self.assertFalse(input_target_known({'pointer_coordinates_match':False,'pointer':{'WINDOW':42}}))
        self.assertTrue(input_target_known({'pointer_coordinates_match':True,'pointer':{'WINDOW':42}}))


if __name__ == '__main__': unittest.main()
