import sys
from pathlib import Path
import unittest
import os
import subprocess
import tempfile
sys.path.insert(0,str(Path(__file__).parents[1]))
from direct_instrumentation import suite_passed


class CompleteSuiteEvidenceTest(unittest.TestCase):
    def success(self):
        return ('INSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_STATUS_CODE: 0\n'*6+
                'INSTRUMENTATION_RESULT: stream=\nOK (6 tests)\nINSTRUMENTATION_CODE: -1\n')
    def test_only_complete_six_case_success_is_accepted(self):
        self.assertTrue(suite_passed(0,self.success()))
        self.assertFalse(suite_passed(1,self.success()))
        self.assertFalse(suite_passed(0,self.success().replace('INSTRUMENTATION_STATUS_CODE: 0\n','',1)))
    def test_failed_or_truncated_instrumentation_is_not_a_pass(self):
        for output in [self.success().replace('INSTRUMENTATION_STATUS_CODE: 0','INSTRUMENTATION_STATUS_CODE: -2',1),
                       self.success().replace('INSTRUMENTATION_CODE: -1',''),
                       self.success()+'INSTRUMENTATION_FAILED: runner crashed\n',
                       self.success().replace('OK (6 tests)','OK (5 tests)')]:
            self.assertFalse(suite_passed(0,output))


    def test_reset_cannot_start_without_both_explicit_fresh_device_guards(self):
        script=Path(__file__).parents[1]/'direct_instrumentation.py'
        with tempfile.TemporaryDirectory() as tmp:
            for flag,fresh in [(True,'0'),(False,'1')]:
                env=os.environ|{'DOCHAGJARI_FRESH_TEST_AVD':fresh,'ANDROID_HOME':'/nonexistent-dochagjari-sdk'}
                cmd=[sys.executable,str(script),'--serial','not-a-real-serial','--output',str(Path(tmp)/'report')]
                if flag:cmd.append('--reset-test-data')
                result=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=10)
                self.assertEqual(2,result.returncode)
                self.assertIn('never use an existing user device',result.stderr)


if __name__=='__main__':unittest.main()
