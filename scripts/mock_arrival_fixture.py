"""Private local-only mock fixture: withhold one response after durable server storage."""
import asyncio
import json
import os
from pathlib import Path
import sys

work = Path(sys.argv[1]).resolve()
os.environ['ARRIVAL_DB'] = str(work / 'outbox.sqlite3')
os.environ['SMS_MODE'] = 'mock'
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from server.app import Settings, create_app
import uvicorn


def forbid_provider(*args):
    (work / 'provider-called').touch()
    raise RuntimeError('Provider must never be called by the mock fixture')


app = create_app(Settings(token=(work / 'token').read_text(), database=str(work / 'outbox.sqlite3'),
                          mode='mock', recipients={'demo-family': {'phone': '0', 'consented': True}}),
                 sender=forbid_provider)


@app.middleware('http')
async def trace_and_hold(request, call_next):
    response = await call_next(request)
    if request.url.path.startswith('/v1/arrivals'):
        with (work / 'events.jsonl').open('a') as log:
            log.write(json.dumps({'method': request.method, 'path': request.url.path, 'status': response.status_code}) + '\n')
        if request.method == 'POST' and response.status_code == 200:
            while (work / 'hold-response').exists():
                await asyncio.sleep(.1)
    response.headers["X-Arrival-Fixture-Pid"] = str(os.getpid())
    return response


uvicorn.run(app, host='127.0.0.1', port=8000, log_level='critical', access_log=False)
