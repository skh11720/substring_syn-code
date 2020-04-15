import re
import os

f = os.popen('ps aux | grep substring-syn | grep -v grep')
try:
    line = f.readline().strip()
    if len(line) == 0: 1 == 0/0 
    attr_list = re.split('\\s+', line)
    assert attr_list[0] == 'hadoop'
    os.system('kill -9 '+attr_list[1])
except:
    pass
