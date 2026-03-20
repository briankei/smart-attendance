#!/usr/bin/env python3
"""NFC Student Attendance Server - serves the app and stores data in CSV."""

import csv
import json
import os
import threading
from datetime import datetime
from http.server import HTTPServer, SimpleHTTPRequestHandler
from urllib.parse import urlparse

DATA_DIR = os.path.dirname(os.path.abspath(__file__))
STUDENT_FILE = os.path.join(DATA_DIR, 'student name')
DATA_FILE = os.path.join(DATA_DIR, 'attendance.csv')
CERT_DIR = os.path.join(DATA_DIR, '..', 'certs')
CLEAR_PASSWORD = os.environ.get('CLEAR_PASSWORD', '')
if not CLEAR_PASSWORD:
    import secrets as _secrets
    CLEAR_PASSWORD = _secrets.token_urlsafe(16)
    print("Clear password generated (set CLEAR_PASSWORD env var to use a fixed one)")
lock = threading.Lock()

# Only serve these files via HTTP
ALLOWED_FILES = {'/', '/index.html', '/manifest.json', '/icon-192.png', '/icon-512.png', '/sw.js'}


def load_student_names():
    names = []
    with open(STUDENT_FILE, 'r', encoding='utf-8') as f:
        for line in f:
            name = line.strip()
            if name:
                names.append(name)
    return names


def load_data():
    """Load attendance data from CSV. Returns dict keyed by student name."""
    names = load_student_names()
    data = {name: {'nfcSerial': '', 'timestamps': []} for name in names}

    if os.path.exists(DATA_FILE):
        with open(DATA_FILE, 'r', encoding='utf-8', newline='') as f:
            reader = csv.DictReader(f)
            for row in reader:
                name = row.get('Name', '').strip()
                if name in data:
                    data[name]['nfcSerial'] = row.get('NFC Serial', '').strip()
                    ts_str = row.get('Timestamps', '').strip()
                    if ts_str:
                        data[name]['timestamps'] = [t.strip() for t in ts_str.split(';') if t.strip()]

    return names, data


def save_data(names, data):
    """Save attendance data to CSV."""
    with open(DATA_FILE, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, quoting=csv.QUOTE_ALL)
        writer.writerow(['Name', 'NFC Serial', 'Timestamps'])
        for name in names:
            d = data.get(name, {'nfcSerial': '', 'timestamps': []})
            ts_str = '; '.join(d['timestamps'])
            writer.writerow([name, d['nfcSerial'], ts_str])


class NFCHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DATA_DIR, **kwargs)

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        if path == '/':
            path = '/index.html'
        if path == '/api/data':
            self._handle_get_data()
        elif path in ALLOWED_FILES:
            super().do_GET()
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path == '/api/register':
            self._handle_register()
        elif parsed.path == '/api/attend':
            self._handle_attend()
        elif parsed.path == '/api/unregister':
            self._handle_unregister()
        elif parsed.path == '/api/clear':
            self._handle_clear()
        elif parsed.path == '/api/manual-attend':
            self._handle_manual_attend()
        else:
            self._send_json(404, {'error': 'Not found'})

    def _read_body(self):
        length = int(self.headers.get('Content-Length', 0))
        if length > 10240:  # 10KB max
            self._send_json(413, {'error': 'Request too large'})
            return {}
        body = self.rfile.read(length)
        return json.loads(body) if body else {}

    def _send_json(self, code, obj):
        data = json.dumps(obj).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _handle_get_data(self):
        with lock:
            names, data = load_data()
        students = []
        for i, name in enumerate(names):
            d = data.get(name, {'nfcSerial': '', 'timestamps': []})
            students.append({
                'id': i,
                'name': name,
                'nfcSerial': d['nfcSerial'],
                'timestamps': d['timestamps']
            })
        self._send_json(200, {'students': students})

    def _handle_register(self):
        body = self._read_body()
        name = body.get('name', '')
        serial = body.get('serial', '')
        if not name or not serial:
            self._send_json(400, {'error': 'Missing name or serial'})
            return

        with lock:
            names, data = load_data()
            if name not in data:
                self._send_json(404, {'error': 'Student not found'})
                return
            # Check if serial already used by another student
            for n, d in data.items():
                if d['nfcSerial'] == serial and n != name:
                    self._send_json(409, {'error': f'Card already registered to {n}'})
                    return
            now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            data[name]['nfcSerial'] = serial
            data[name]['timestamps'].append(now)
            save_data(names, data)

        self._send_json(200, {'ok': True, 'timestamp': now})

    def _handle_attend(self):
        body = self._read_body()
        serial = body.get('serial', '')
        if not serial:
            self._send_json(400, {'error': 'Missing serial'})
            return

        with lock:
            names, data = load_data()
            found_name = None
            for n, d in data.items():
                if d['nfcSerial'] == serial:
                    found_name = n
                    break
            if not found_name:
                self._send_json(404, {'error': 'Card not registered'})
                return
            now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            data[found_name]['timestamps'].append(now)
            save_data(names, data)

        self._send_json(200, {'ok': True, 'name': found_name, 'timestamp': now})

    def _handle_unregister(self):
        body = self._read_body()
        name = body.get('name', '')
        if not name:
            self._send_json(400, {'error': 'Missing name'})
            return

        with lock:
            names, data = load_data()
            if name not in data:
                self._send_json(404, {'error': 'Student not found'})
                return
            data[name]['nfcSerial'] = ''
            save_data(names, data)

        self._send_json(200, {'ok': True})

    def _handle_manual_attend(self):
        body = self._read_body()
        name = body.get('name', '')
        if not name:
            self._send_json(400, {'error': 'Missing name'})
            return

        with lock:
            names, data = load_data()
            if name not in data:
                self._send_json(404, {'error': 'Student not found'})
                return
            now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            data[name]['timestamps'].append(now)
            save_data(names, data)

        self._send_json(200, {'ok': True, 'name': name, 'timestamp': now})

    def _handle_clear(self):
        body = self._read_body()
        password = body.get('password', '')
        if password != CLEAR_PASSWORD:
            self._send_json(403, {'error': 'Wrong password'})
            return

        with lock:
            names, data = load_data()
            for name in data:
                data[name]['nfcSerial'] = ''
                data[name]['timestamps'] = []
            save_data(names, data)

        self._send_json(200, {'ok': True})

    def log_message(self, format, *args):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {args[0]}")


class HTTPSRedirectHandler(SimpleHTTPRequestHandler):
    """Redirects all HTTP requests to HTTPS."""
    def do_GET(self):
        host = self.headers.get('Host', 'localhost').split(':')[0]
        self.send_response(301)
        self.send_header('Location', f'https://{host}:8443{self.path}')
        self.end_headers()
    def do_POST(self):
        self.do_GET()


def main():
    import ssl

    bind_addr = os.environ.get('BIND_ADDR', '0.0.0.0')
    port_http = 8090
    port_https = 8443
    cert_file = os.path.join(CERT_DIR, 'cert.pem')
    key_file = os.path.join(CERT_DIR, 'key.pem')

    # HTTP server only redirects to HTTPS
    http_server = HTTPServer((bind_addr, port_http), HTTPSRedirectHandler)
    http_thread = threading.Thread(target=http_server.serve_forever, daemon=True)
    http_thread.start()
    print(f"HTTP  server on {bind_addr}:{port_http} (redirects to HTTPS)")

    # HTTPS server
    https_server = HTTPServer((bind_addr, port_https), NFCHandler)
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.minimum_version = ssl.TLSVersion.TLSv1_2
    ctx.load_cert_chain(cert_file, key_file)
    https_server.socket = ctx.wrap_socket(https_server.socket, server_side=True)
    print(f"HTTPS server on {bind_addr}:{port_https}")
    https_server.serve_forever()


if __name__ == '__main__':
    main()
