from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]))
from host_touch import project_point


class ObservedViewportProjectionTest(unittest.TestCase):
    window={'x':100,'y':100,'width':2400,'height':1350}
    def test_actual_viewport_and_pleos_xml_coordinates_project_to_expected_point(self):
        self.assertEqual([1067,289],project_point(self.window,(2560,1440),(1031,202)))
    def test_unknown_or_inconsistent_geometry_is_rejected(self):
        for size,point in [((1280,800),(1031,202)),((2560,1440),(2560,202)),((0,1440),(1,1))]:
            with self.assertRaises(ValueError):project_point(self.window,size,point)


if __name__=='__main__':unittest.main()
