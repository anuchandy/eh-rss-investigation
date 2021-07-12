import subprocess
import sys
import os
import time
import re
from datetime import datetime

_RE_COMBINE_WHITESPACE = re.compile(r"\s+")

def docker_ps_command(containerId):
    fileName = 'rss-captured-' + containerId + '.csv'
    outputFile = open(fileName, 'w')
    outputFile.write("TIME,PID,USER,VSZ,RSS,COMMAND\n");
    print("file created:" + fileName);
    outputFile.close()
    while(True):
        cmd = 'docker exec -it ' +  containerId + ' ps -o pid,user,vsz,rss,comm'
        pipe = subprocess.Popen([cmd], shell=True, stdout = subprocess.PIPE)
        output = str(pipe.communicate())
        jva_line = output.split("\\r\\n")
        entry = datetime.now().strftime("%m/%d/%Y %H:%M:%S") + "," + _RE_COMBINE_WHITESPACE.sub(",", (jva_line[1]).strip())
        outputFile = open(fileName, 'a')
        outputFile.write(entry + "\n");
        outputFile.close()
        time.sleep(10 * 60)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit('capture-container-rss.py <containerId>')
    docker_ps_command(sys.argv[1])