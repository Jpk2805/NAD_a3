#  
#  FILE          : server.py
#  PROJECT       : Assignment #3 
#  PROGRAMMER    : Ayushkumar Rakholiya & Jaykumar Patel 
#  FIRST VERSION : 205-02-20 
#  DESCRIPTION   : 
#   this file handles incoming log messages from clients.
#

import os
import threading
import io
import socket
import json
from collections import defaultdict
from datetime import datetime, timedelta
import csv

class Constants(object):
    LOGGING_SERVICE_IP = "127.0.0.1"
    MAX_LOG_LENGTH = 4096
    LOGGING_PORT = 8089
    SOCKET_TIMEOUT = 5
    LOGGING_FOLDERS = "logs"
    RATE_LIMIT = 10
    RATE_WINDOW = 60 
    RATE_CONNECTIONS = 5

CONST = Constants()

'''
    Settings class
    This class is used to get the settings from the config file
    If the config file is not found, it will create a default config file
    If the config file is found, it will get the settings from the config file
'''
class Settings():
    def sGet():
        print("Getting from config file...")
        try:
            with open("config.json") as config_file:
                js = json.load(config_file)
                CONST.LOGGING_SERVICE_IP  = js["LOGGING_SERVICE_IP"]
                CONST.MAX_LOG_LENGTH      = js["MAX_LOG_LENGTH"]
                CONST.LOGGING_PORT        = js["LOGGING_PORT"]
                CONST.SOCKET_TIMEOUT      = js["SOCKET_TIMEOUT"]
                CONST.LOGGING_FOLDERS     = js["LOGGING_FOLDERS"]
                CONST.RATE_LIMIT          = js.get("RATE_LIMIT", 10)
                CONST.RATE_WINDOW         = js.get("RATE_WINDOW", 60)
                CONST.RATE_CONNECTIONS    = js.get("RATE_CONNECTIONS", 5)
        except Exception as e:
            print("Error: Invalid configuration file with message -> %s" % str(e))
            print("Restoring the defaults to config.json, invalid config file is moved to 'ERROR_config.json' for future use")
            os.rename("config.json", "ERROR_config.json")
            Settings.sSetDefault()

    def sSetDefault():
        print("Setting default Config file...")
        if not os.path.isfile("config.json"):
            with open("config.json", "w") as newConfig:
                settings = {
                    "LOGGING_SERVICE_IP"    : "127.0.0.1",
                    "MAX_LOG_LENGTH"        : 1024,
                    "LOGGING_PORT"          : 8089,
                    "SOCKET_TIMEOUT"        : 5,
                    "LOGGING_FOLDERS"       : "logs",
                    "RATE_LIMIT"            : 10,
                    "RATE_WINDOW"           : 60,
                    "RATE_CONNECTIONS"      : 5
                }
                json.dump(settings, newConfig, indent=4, sort_keys=True)
        print("Config file created.")

    def sPrint():
        with open("config.json") as configFile:
            js = json.load(configFile)
            print(json.dumps(js, indent=4, sort_keys=True))

# this function is used to generate the response for the client
def generateResponse(code, response):
    x = ("{\"code\":\"%s\",\"message\":\"%s\"}" % (str(code), str(response)))
    print(x)
    return x.encode('utf-8')

clientLogs = defaultdict(list)
clientLock = threading.Lock()

# this function will be used to process the message from the client
def executeMessage(address, message):
    isValidMessage = True
    returnMessage = ""

    print(address, json.dumps(message, indent=4, sort_keys=True))

    if message["Action"] == "Log":
        currentTime = datetime.now()
        clientIp = address[0]  # Extract the IP from the address tuple

        with clientLock:  # Acquire lock to prevent race conditions
            # Append the current log time and filter old entries
            clientLogs[clientIp].append(currentTime)
            clientLogs[clientIp] = [logTime for logTime in clientLogs[clientIp] 
                                      if currentTime - logTime < timedelta(seconds=CONST.RATE_WINDOW)]
            
            # Check rate limit after updates
            if len(clientLogs[clientIp]) > CONST.RATE_LIMIT:
                returnMessage = "Rate limit exceeded. Try again later."
                isValidMessage = False
            else:
                try:
                    # opening the log file in append mode from path specified in the constant config.json file 
                    logFilePath = os.path.join(CONST.LOGGING_FOLDERS, datetime.today().strftime('%Y-%m-%d') + ".LOG")
                    with open(logFilePath, "a") as logFile:
                        logFile.write(generateLogLine(message["Parameters"], address))
                    returnMessage = str(len(message))
                except Exception as e:
                    returnMessage = f"Error writing log: {str(e)}"
                    isValidMessage = False
    else:
        returnMessage = "Invalid Action"

    return isValidMessage, returnMessage

# this function will be used to generate the log line
def generateLogLine(parameters, address):
    logFormat = parameters.get("Format", "text").lower()
    if logFormat not in ["text", "json", "csv"]:
        logFormat = "text"

    if logFormat == "json":
        logEntry = {
            "timestamp": parameters["Timestamp"],
            "clientIp": address[0],
            "clientPort": address[1],
            "level": parameters["Level"],
            "message": parameters["Message"],
            "fileName": parameters["FileName"],
            "fileLine": parameters["FileLine"],
            "tags": parameters["Tags"]
        }
        return json.dumps(logEntry) + "\n"
    elif logFormat == "csv":
        # Using csv module to ensure proper CSV formatting and escaping.
        output = io.StringIO()
        writer = csv.writer(output)
        writer.writerow([
            parameters["Timestamp"],
            address[0],
            address[1],
            parameters["Level"],
            parameters["Message"],
            parameters["FileName"],
            parameters["FileLine"],
            ";".join(parameters["Tags"])
        ])
        return output.getvalue()
    else:
        allTags = ", ".join(parameters["Tags"])
        return "%s %s:%d [%s] - %s (%s:%d)[%s]\n" % (
            parameters["Timestamp"],
            address[0],
            address[1],
            parameters["Level"],
            parameters["Message"],
            parameters["FileName"],
            parameters["FileLine"],
            allTags
        )

clientLogs = defaultdict(list)

# this function will be used to receive the messages from the client
def recieveMessages(connection, address):
    print("Connection established with %s" % str(address))
    while True:
        try:
            message = connection.recv(CONST.MAX_LOG_LENGTH)
            if not message:
                print("Connection broken with %s" % str(address))
                break

            decoded_message = message.decode("utf-8")
            try:
                # Try to parse the message as JSON.
                parsed_message = json.loads(decoded_message)
                # Process as a structured log message.
                isValidMessage, returnMessage = executeMessage(address, parsed_message)
            except json.JSONDecodeError:
                # If the message isn’t valid JSON, assume it’s a preformatted log message.
                logFilePath = os.path.join(CONST.LOGGING_FOLDERS,
                                            datetime.today().strftime('%Y-%m-%d') + ".LOG")
                with open(logFilePath, "a") as logFile:
                    logFile.write(decoded_message + "\n")
                isValidMessage = True
                returnMessage = "Logged pre-formatted message"
            
            if isValidMessage:
                connection.send(generateResponse(0, returnMessage))
            else:
                connection.send(generateResponse(-1, returnMessage))

        except socket.timeout:
            continue
        except Exception as e:
            print("Error:", e)
            break


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
    if os.path.isfile("config.json"):
        Settings.sGet()
    else:
        Settings.sSetDefault()

    Settings.sPrint()

    loggingSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    loggingSocket.bind((CONST.LOGGING_SERVICE_IP, CONST.LOGGING_PORT))
    loggingSocket.listen(CONST.RATE_CONNECTIONS)  # Maximum connections as per settings

    # Create the base logging folder to store all the logs
    if not os.path.isdir(CONST.LOGGING_FOLDERS):
        os.mkdir(CONST.LOGGING_FOLDERS)

    print("Program initialized, listening for logging requests...")

    while True:
        try:
            conn, addr = loggingSocket.accept()
            print("Connection from %s" % str(addr))
            # using threading to handle multiple clients.
            threading.Thread(target=recieveMessages, args=(conn, addr)).start()
        except socket.timeout:
            # on socket timeout, restart the server
            print("Socket timeout, restarting the server...")
            loggingSocket.close()
            loggingSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            loggingSocket.bind((CONST.LOGGING_SERVICE_IP, CONST.LOGGING_PORT))
            loggingSocket.listen(CONST.RATE_CONNECTIONS)
            print("Server restarted, listening for logging requests...")
        except Exception as e:
            print("Error: %s" % str(e))
            loggingSocket.close()
