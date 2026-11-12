"""Read-only native X11 window visibility/input-region snapshot. No focus or input changes."""
import ctypes as C
import ctypes.util
import json
import sys


class Attributes(C.Structure):
    _fields_ = [('x',C.c_int),('y',C.c_int),('width',C.c_int),('height',C.c_int),('border_width',C.c_int),
                ('depth',C.c_int),('visual',C.c_void_p),('root',C.c_ulong),('window_class',C.c_int),
                ('bit_gravity',C.c_int),('win_gravity',C.c_int),('backing_store',C.c_int),
                ('backing_planes',C.c_ulong),('backing_pixel',C.c_ulong),('save_under',C.c_int),
                ('colormap',C.c_ulong),('map_installed',C.c_int),('map_state',C.c_int),
                ('all_event_masks',C.c_long),('your_event_mask',C.c_long),('do_not_propagate_mask',C.c_long),
                ('override_redirect',C.c_int),('screen',C.c_void_p)]


class Rectangle(C.Structure):
    _fields_ = [('x',C.c_short),('y',C.c_short),('width',C.c_ushort),('height',C.c_ushort)]


class Error(C.Structure):
    _fields_ = [('type',C.c_int),('display',C.c_void_p),('resourceid',C.c_ulong),('serial',C.c_ulong),
                ('error_code',C.c_ubyte),('request_code',C.c_ubyte),('minor_code',C.c_ubyte)]


class NativeDisplay:
    def __init__(self):
        self.lib = C.CDLL(ctypes.util.find_library('X11'))
        self.shape = C.CDLL(ctypes.util.find_library('Xext'))
        self.lib.XOpenDisplay.argtypes=[C.c_char_p]; self.lib.XOpenDisplay.restype=C.c_void_p
        self.lib.XCloseDisplay.argtypes=[C.c_void_p]
        self.lib.XSync.argtypes=[C.c_void_p,C.c_int]
        self.lib.XFree.argtypes=[C.c_void_p]
        self.lib.XDefaultRootWindow.argtypes=[C.c_void_p]; self.lib.XDefaultRootWindow.restype=C.c_ulong
        self.lib.XGetWindowAttributes.argtypes=[C.c_void_p,C.c_ulong,C.POINTER(Attributes)]
        self.lib.XTranslateCoordinates.argtypes=[C.c_void_p,C.c_ulong,C.c_ulong,C.c_int,C.c_int,
                                                 C.POINTER(C.c_int),C.POINTER(C.c_int),C.POINTER(C.c_ulong)]
        self.lib.XQueryPointer.argtypes=[C.c_void_p,C.c_ulong,C.POINTER(C.c_ulong),C.POINTER(C.c_ulong),
                                        C.POINTER(C.c_int),C.POINTER(C.c_int),C.POINTER(C.c_int),
                                        C.POINTER(C.c_int),C.POINTER(C.c_uint)]
        self.shape.XShapeQueryVersion.argtypes=[C.c_void_p,C.POINTER(C.c_int),C.POINTER(C.c_int)]
        self.shape.XShapeGetRectangles.argtypes=[C.c_void_p,C.c_ulong,C.c_int,C.POINTER(C.c_int),C.POINTER(C.c_int)]
        self.shape.XShapeGetRectangles.restype=C.POINTER(Rectangle)
        self.errors=[]
        self.callback=C.CFUNCTYPE(C.c_int,C.c_void_p,C.POINTER(Error))(self._error)
        self.lib.XSetErrorHandler.argtypes=[C.c_void_p];self.lib.XSetErrorHandler.restype=C.c_void_p
        self.old_handler=self.lib.XSetErrorHandler(C.cast(self.callback,C.c_void_p))
        self.display=self.lib.XOpenDisplay(None)
        if not self.display:
            self.lib.XSetErrorHandler(self.old_handler)
            raise RuntimeError('Private X11 display could not be opened')
        self.root=self.lib.XDefaultRootWindow(self.display)

    def _error(self,display,event):
        e=event.contents
        self.errors.append({'resource':e.resourceid,'error':e.error_code,'request':e.request_code})
        return 0

    def close(self):
        self.lib.XCloseDisplay(self.display)
        self.lib.XSetErrorHandler(self.old_handler)

    def snapshot(self,windows,point):
        major,minor=C.c_int(),C.c_int()
        extension=bool(self.shape.XShapeQueryVersion(self.display,C.byref(major),C.byref(minor)))
        shape_supported=extension and (major.value,minor.value)>=(1,1)
        root,child=C.c_ulong(),C.c_ulong()
        rx,ry,wx,wy=C.c_int(),C.c_int(),C.c_int(),C.c_int();mask=C.c_uint()
        same_screen=self.lib.XQueryPointer(self.display,self.root,C.byref(root),C.byref(child),
            C.byref(rx),C.byref(ry),C.byref(wx),C.byref(wy),C.byref(mask))
        report={'readonly':True,'root':self.root,'shape_version':[major.value,minor.value],
                'direct_pointer':{'same_screen':bool(same_screen),'child':child.value,'x':rx.value,'y':ry.value},'windows':[]}
        for window in dict.fromkeys(windows):
            item={'id':window};attrs=Attributes()
            ok=self.lib.XGetWindowAttributes(self.display,window,C.byref(attrs));self.lib.XSync(self.display,0)
            if not ok:
                item['attributes_available']=False;report['windows'].append(item);continue
            item.update(attributes_available=True,map_state=attrs.map_state,
                        map_state_name={0:'IsUnmapped',1:'IsUnviewable',2:'IsViewable'}.get(attrs.map_state,'unknown'),
                        window_class=attrs.window_class,width=attrs.width,height=attrs.height,
                        override_redirect=bool(attrs.override_redirect))
            px,py=C.c_int(),C.c_int();at=C.c_ulong()
            translated=self.lib.XTranslateCoordinates(self.display,self.root,window,point[0],point[1],
                                                       C.byref(px),C.byref(py),C.byref(at))
            item['requested_point_local']=[px.value,py.value] if translated else None
            if shape_supported:
                for label,kind in [('input',2),('bounding',0)]:
                    count,order=C.c_int(),C.c_int()
                    rectangles=self.shape.XShapeGetRectangles(self.display,window,kind,C.byref(count),C.byref(order))
                    self.lib.XSync(self.display,0)
                    regions=[dict(x=rectangles[i].x,y=rectangles[i].y,width=rectangles[i].width,height=rectangles[i].height)
                             for i in range(min(count.value,32))] if rectangles else []
                    if rectangles:self.lib.XFree(rectangles)
                    item.update({label+'_rectangles_count':count.value,label+'_rectangles':regions,
                                 label+'_regions_complete':count.value<=32,
                                 'point_in_'+label+'_region':bool(translated) and any(r['x']<=px.value<r['x']+r['width'] and
                                            r['y']<=py.value<r['y']+r['height'] for r in regions)})
            report['windows'].append(item)
        self.lib.XSync(self.display,0)
        report['x11_errors']=list(self.errors)
        return report


if __name__=='__main__':
    try:
        windows=[int(value) for value in sys.argv[1].split(',') if value][:12]
        point=(int(sys.argv[2]),int(sys.argv[3]))
        display=NativeDisplay()
        try:result=display.snapshot(windows,point)
        finally:display.close()
    except Exception as error:
        result={'readonly':True,'available':False,'error_type':type(error).__name__}
    print(json.dumps(result))
