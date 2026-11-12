from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]))
from dialog_identity import identity,same_frame_header_confirms


class SameFrameHeaderTest(unittest.TestCase):
    fp='AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99'
    def full(self,title='Allow USB debugging?'):
        return identity(title+' computer 654 9 '+self.fp,self.fp)
    def header(self,fp=None):
        return identity('컴퓨터 RSA 키 지문: '+(fp or self.fp),self.fp)
    def test_small_label_miss_requires_matching_original_and_header_identity(self):
        full=self.full();header=self.header()
        self.assertFalse(full['dialog_detected'])
        self.assertTrue(same_frame_header_confirms(full,header))
    def test_other_host_or_duplicate_fingerprint_is_rejected(self):
        for header in [self.header(self.fp.replace('AA','AB')),self.header(self.fp+' '+self.fp)]:
            self.assertFalse(same_frame_header_confirms(self.full(),header))
    def test_missing_usb_title_or_original_identity_is_rejected(self):
        self.assertFalse(same_frame_header_confirms(self.full('Allow camera?'),self.header()))
        self.assertFalse(same_frame_header_confirms(identity('Allow USB debugging?',self.fp),self.header()))
    def test_missing_header_labels_is_rejected(self):
        self.assertFalse(same_frame_header_confirms(self.full(),identity('컴퓨터 '+self.fp,self.fp)))


if __name__=='__main__':unittest.main()
