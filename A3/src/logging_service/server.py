import os, sys, io, selectors
import socket


class Constants(object):
    LOGGING_SERVICE_IP = "127.0.0.1"
    MAX_LOG_LENGTH = 4096
    LOGGING_PORT = 8089
    SOCKET_TIMEOUT = 5
    LOGGING_FOLDERS = "logs"
    RATE_LIMIT = 10
    RATE_WINDOW = 60 

CONST = Constants()

class SimpleSocket(socket.socket):
    def __init__(self, family=socket.AF_INET, type=socket.SOCK_STREAM, proto=0, fileno=None):
        super().__init__(family, type, proto, fileno)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def __repr__(self):
        try:
            fd = self.fileno()
        except socket.error:
            fd = -1
        return (f"<SimpleSocket fd={fd} family={self.family} "
                f"type={self.type} proto={self.proto}>")

def create_connection(address, timeout=None):
    host, port = address
    lastErr = None
    for res in socket.getaddrinfo(host, port, socket.AF_UNSPEC, socket.SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        try:
            s = SimpleSocket(af, socktype, proto)
            if timeout is not None:
                s.settimeout(timeout)
            s.connect(sa)
            return s 
        except OSError as e:
            lastErr = e
            s.close()
    if lastErr:
        raise lastErr
    raise OSError("getaddrinfo returned an empty list")

def create_server(address, backlog=5):
    s = SimpleSocket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(address)
    s.listen(backlog)
    return s

if __name__ == "__main__":
    server = create_server(('localhost', 0))
    print("Server listening on", server.getsockname())

    server.close()