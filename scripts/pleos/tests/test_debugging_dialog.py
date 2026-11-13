from pathlib import Path
import sys
import unittest
import base64
from unittest.mock import patch
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).parents[1]))
import debugging_dialog
from dialog_regions import permission_button_pair


class DebuggingDialogTest(unittest.TestCase):
    fingerprint = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"

    def screen(self, title="Allow USB debugging?", fingerprint=None, buttons=None):
        groups = [title.split(), ["RSA", "key", "fingerprint", fingerprint or self.fingerprint],
                  ["Always", "allow", "from", "this", "computer"]] + (buttons or [["Cancel", "Allow"]])
        out = ["block_num\tpar_num\tline_num\tleft\ttop\twidth\theight\tconf\ttext"]
        for block, words in enumerate(groups, 1):
            for i, word in enumerate(words):
                out.append(f"{block}\t1\t1\t{100+i*100}\t{100+block*50}\t80\t20\t95\t{word}")
        return "\n".join(out)

    def test_normal_allow_requires_matching_host_fingerprint_and_not_checkbox(self):
        plan = debugging_dialog.approval_plan(self.screen(), self.fingerprint)
        self.assertEqual((240, 310), plan["click"])
        self.assertTrue(plan["dialog_detected"])

    def test_wrong_host_or_unrelated_permission_never_clicks(self):
        for screen in [self.screen(fingerprint=self.fingerprint.replace("AA", "AB")),
                       self.screen(fingerprint=self.fingerprint + ":AA"),
                       self.screen(title="Allow camera permission?")]:
            self.assertIsNone(debugging_dialog.approval_plan(screen, self.fingerprint)["click"])

    def test_ambiguous_allow_and_checkbox_only_never_click(self):
        for buttons in [[["Cancel", "Allow"], ["Cancel", "Allow"]], [["Always", "allow", "from", "this", "computer"]], [["Cancel"], ["allow"]]]:
            self.assertIsNone(debugging_dialog.approval_plan(self.screen(buttons=buttons), self.fingerprint)["click"])

    def test_korean_normal_debugging_dialog_uses_same_fingerprint_gate(self):
        screen = self.screen(title="USB 디버깅을 허용하시겠습니까?", buttons=[["취소", "허용"]])
        screen = screen.replace("\tkey\n", "\t키\n").replace("\tfingerprint\n", "\t지문\n")
        self.assertIsNotNone(debugging_dialog.approval_plan(screen, self.fingerprint)["click"])

    def test_region_buttons_require_a_unique_cancel_allow_bar(self):
        buttons = [{"label": "cancel", "box": (100, 200, 148, 45)},
                   {"label": "allow", "box": (260, 200, 148, 45)}]
        self.assertEqual((334, 222), permission_button_pair(buttons))
        self.assertIsNone(permission_button_pair(buttons + [buttons[1]]))
        self.assertIsNone(permission_button_pair([buttons[1]]))
        self.assertIsNone(permission_button_pair([buttons[0], {"label": "allow", "box": (260, 400, 148, 45)}]))

    def test_post_input_observation_never_sends_another_input(self):
        with patch.object(debugging_dialog.subprocess, "run", return_value=SimpleNamespace(stdout=self.screen())) as run:
            result = debugging_dialog.observe_after_input(Path("after.png"), {}, self.fingerprint)
        self.assertTrue(result["dialog_detected"])
        self.assertTrue(result["host_fingerprint_matches"])
        self.assertNotIn("click", result)
        self.assertEqual(["import", "tesseract"], [call.args[0][0] for call in run.call_args_list])

    def test_post_input_ocr_miss_does_not_prove_dialog_dismissal(self):
        with patch.object(debugging_dialog.subprocess, "run", return_value=SimpleNamespace(stdout="")):
            result = debugging_dialog.observe_after_input(Path("after.png"), {}, self.fingerprint)
        self.assertEqual("unknown", result["dialog_state"])
        self.assertFalse(result["absence_verified"])

    def test_native_observation_is_not_skipped_when_title_ocr_misses(self):
        public = base64.b64encode(b'synthetic public host fixture').decode()
        plan = dict(dialog_detected=False,host_fingerprint_matches=True,unambiguous_allow_button=False,click=None)
        regions = dict(notice_click=None,desktop_panel_blocked=False,permission_click=(2403,351),buttons=[])
        observed = dict(readonly=True,native_state={'windows':[]})
        with patch.object(debugging_dialog.Path,'read_text',return_value=public), \
             patch.object(debugging_dialog.subprocess,'run',return_value=SimpleNamespace(stdout='',returncode=0,stderr='')) as run, \
             patch.object(debugging_dialog,'approval_plan',return_value=plan), \
             patch.object(debugging_dialog,'inspect_regions',return_value=regions), \
             patch.object(debugging_dialog,'observe_pointer',return_value=observed) as observe:
            result=debugging_dialog.approve_visible_dialog(Path('/tmp'),Path('/tmp'),{},1)
        observe.assert_called_once_with({},(2403,351))
        self.assertEqual(observed,result['x11_observation'])
        self.assertFalse(result['clicked'])
        self.assertEqual(['import','tesseract'],[call.args[0][0] for call in run.call_args_list])


if __name__ == "__main__":
    unittest.main()
