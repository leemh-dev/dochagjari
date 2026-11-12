"""Resolve a visible labeled toggle by accessibility semantics and row geometry."""
import re


def rectangle(node):
    values = tuple(map(int, re.findall(r'\d+', node.attrib.get('bounds', ''))))
    if len(values) != 4 or values[2] <= values[0] or values[3] <= values[1]:
        return None
    return values


def labeled_toggle(nodes, label):
    names = [n for n in nodes if n.attrib.get('text') == label and rectangle(n)]
    if len(names) != 1:
        raise RuntimeError('Visible toggle label is missing or ambiguous: ' + label)
    _, y1, right, y2 = rectangle(names[0])
    candidates = []
    for node in nodes:
        b = rectangle(node)
        if b and node.attrib.get('checkable') == 'true' and node.attrib.get('clickable') == 'true':
            x, top, _, bottom = b
            if x >= right - 1 and abs((top + bottom) / 2 - (y1 + y2) / 2) <= min(bottom - top, y2 - y1) / 2:
                candidates.append(node)
    if len(candidates) != 1:
        raise RuntimeError('Cannot safely identify labeled toggle: ' + label)
    return candidates[0]
