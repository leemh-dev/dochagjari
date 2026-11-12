"""Strict same-frame USB-dialog identity checks; never exposes key material."""
import re


def identity(text,fingerprint):
    text=text.casefold();compact=''.join(text.split())
    title='allow usb debugging' in text or ('usb' in compact and '디버깅' in compact and '허용' in compact)
    rsa='rsa' in text
    label='fingerprint' in text or '지문' in text
    found=re.findall(r'(?<![0-9A-F:])(?:[0-9A-F]{2}\s*[:;]\s*){15}[0-9A-F]{2}(?![0-9A-F:])',text.upper())
    matches=len(found)==1 and re.sub(r'\s','',found[0]).replace(';',':')==fingerprint.upper()
    return dict(dialog_detected=title and rsa and label,title_detected=title,rsa_marker_detected=rsa,
                fingerprint_label_detected=label,fingerprint_count=len(found),host_fingerprint_matches=matches)


def same_frame_header_confirms(full,header):
    # Full frame must already show the USB title and exactly this host's fingerprint.
    # The same original's enlarged header must independently repeat that fingerprint and both labels.
    return (full.get('title_detected') is True and full.get('host_fingerprint_matches') is True and
            full.get('fingerprint_count')==1 and header.get('rsa_marker_detected') is True and
            header.get('fingerprint_label_detected') is True and header.get('host_fingerprint_matches') is True and
            header.get('fingerprint_count')==1)
