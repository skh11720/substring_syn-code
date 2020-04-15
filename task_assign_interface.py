from collections import defaultdict
import curses
import datetime
import glob
import logging
import os
import time
import subprocess
import sys
import time

import paramiko

from collections import deque
from paramiko import SSHClient
from subprocess import Popen, DEVNULL


REMOTE_HOME = '~/ghsong/substring_syn/'
CMD_CHECK_STATUS = 'ps aux | grep substring_syn | grep -v grep'
REFRESH_PERIOD = 0.1
REFRESH_PERIOD_NS = REFRESH_PERIOD * 1e9

Y_CLIENT = 3
TAB_SIZE = 4
TEXT_WIDTH = 200



class Worker:
    def __init__(self, host):
        self.host = host
        self.ssh = connect(host)


    def __repr__(self):
        return self.host


def connect(host):
    ssh = SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy()) # NOT RECOMMENDED
    pkey_path = os.path.expanduser('~/.ssh/id_rsa')
    mykey = paramiko.RSAKey.from_private_key_file(pkey_path)
    try: ssh.connect(hostname=host, username='hadoop', timeout=1, banner_timeout=1)
    except: 
        print('Connection to '+host+': FAILED')
        return None
    else: 
        print('Connection to '+host+': OK')
        return ssh


def get_worker_list(n_max_worker):
    worker_list = [Worker('cherry{:02d}'.format(idx+1)) for idx in range(n_max_worker)]
    worker_list = filter(lambda w: w.ssh is not None, worker_list)
    worker_list = filter(lambda w: w.host not in worker_blacklist, worker_list)
    #try: assert len(ssh_list) == 38
    #except: AssertionError('len(ssh_list) == %d != 38'%len(ssh_list))
    #for ssh in ssh_list: exec_command(ssh, 'cd '+REMOTE_HOME)
    return list(worker_list)


def close_all(worker_list):
    [worker.ssh.close() for worker in worker_list]


def run_command(worker, cmd, get_output=True):
    ssh = worker.ssh
    if ssh.get_transport() is not None and ssh.get_transport().is_active():
        cmd = 'cd '+REMOTE_HOME+'; '+cmd
        if get_output:
            return run_command_and_return_output(ssh, cmd)
        else:
            Popen(["ssh %s \"%s\""%(worker.host, cmd)], shell=True, stdin=None, stdout=DEVNULL, stderr=DEVNULL, close_fds=True)
    else: raise Exception('SSH TRANSFORT EXCEPTION')


def run_command_and_return_output(ssh, cmd):
    try:
        _, stdout, stderr = ssh.exec_command(cmd)
        output = stdout.read().decode('utf8').strip()
    except: output = None
    return output


def get_status_all(worker_list):
    return [get_status(worker) for worker in worker_list]


def get_status(worker):
    try: 
        output = run_command(worker, CMD_CHECK_STATUS)
        if output is None: return 'ERROR'
        token_list = list(filter(lambda x:x!='', output.split(' ')))
        if output.startswith('hadoop'): return 'RUN'
        else: return 'WAIT'
    #except paramiko.ssh_exception.SSHException: 
    except Exception:
        return 'ERROR'


def build_job_queue(path):
    job_queue = deque([])
    with open(path) as f:
        for line in f:
            if line.startswith('#'): continue
            job_queue.append(line.strip())
    return job_queue


def get_next_job(job_queue):
    if len(job_queue) > 0: return job_queue[0]
    else: return "None"


def is_terminated_all(status_list):
    return all(map(lambda x:x == 'WAIT', status_list))


#def assign_jobs(worker_list, job_queue):
#    widx = 0
#    for i in range(200):
#        print('NUMBER OF JOBS:', len(job_queue))
#        worker = worker_list[widx]
#        status = get_status(worker)
#        print(worker.host+' STATUS: '+status)
#        if status == 'WAIT':
#            job = job_queue.popleft()
#            run_command(worker, job, False)
#        time.sleep(REFRESH_PERIOD)
#        widx = (widx+1)%len(worker_list)
#        if len(job_queue) == 0: break
#    print('FINISHED')




class ScreenStat:
    def __init__(self):
        self.k = 0
        self.x = 0
        self.y = 0
    

    def update(self, stdscr):
        self.y_max, self.x_max = stdscr.getmaxyx()
        if self.k in [curses.KEY_DOWN, ord('j')]: self.y = min(self.y+1, self.y_max)
        elif self.k in [curses.KEY_UP, ord('k')]: self.y = max(self.y-1, 0)
        elif self.k in [curses.KEY_RIGHT, ord('l')]: self.x = min(self.x+1, self.x_max)
        elif self.k in [curses.KEY_LEFT, ord('h')]: self.x = max(self.x-1, 0)



def update_status(widx, new_state):
    prev_status_list[widx] = status_list[widx]
    status_list[widx] = new_state


def getLogger(inputname):
    FORMAT = "%(asctime)s: %(message)s"
    formatter = logging.Formatter(FORMAT)
    logging.basicConfig(format=FORMAT)
    logger = logging.getLogger('logger')
    for hdr in logger.handlers: logger.removeHandler(hdr)
    if os.path.exists(inputname+'.log'): os.remove(inputname+'.log')
    fhdr = logging.FileHandler(inputname+'.log')
    fhdr.setFormatter(formatter)
    logger.addHandler(fhdr)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    return logger


def init_screen(stdscr):
    stdscr.clear()
    stdscr.refresh()
    stdscr.nodelay(True)

    curses.curs_set(0)
    curses.start_color()
    curses.init_pair(1, curses.COLOR_WHITE, curses.COLOR_BLACK)
    curses.init_pair(2, curses.COLOR_GREEN, curses.COLOR_BLACK)
    curses.init_pair(3, curses.COLOR_BLUE, curses.COLOR_BLACK)
    curses.init_pair(4, curses.COLOR_RED, curses.COLOR_BLACK)


def get_size_of_worker_window(NUM_CLIENTS_IN_ROW=10, NUM_CLIENTS_IN_COL=None):
    if NUM_CLIENTS_IN_ROW is not None:
        num_row = NUM_CLIENTS_IN_ROW
        num_col = (len(worker_list)+num_row-1)//num_row
    elif NUM_CLIENTS_IN_COL is not None:
        num_col = 3
        num_row = (len(worker_list)+num_col-1)//num_col
    else: raise Exception("ERROR: worker list size")
    return num_row, num_col


def draw_worker_list(stdscr):
    y0 = Y_CLIENT
    x0 = TAB_SIZE
    stdscr.addstr(y0, x0, "=== worker list ===")
    y0 += 1
    for widx, worker in enumerate(worker_list):
        draw_worker(stdscr, y0, x0, widx)


def draw_worker(stdscr, y0, x0, widx):
    worker = worker_list[widx]
    status = status_list[widx]
    y = y0+1+(widx%NUM_CLIENTS_IN_ROW)
    x = x0+20*(widx//NUM_CLIENTS_IN_ROW)
    stdscr.addstr(y, x, "  "+worker.host)
    if status == "NONE": color_code = 1
    elif status == "WAIT": color_code = 2
    elif status == "RUN": color_code = 3
    elif status == "ERROR": color_code = 4
    else:
        status = "UNK"
        color_code = 1
    stdscr.addstr(y, x+4+len(worker.host), "{:8s}".format(status), curses.color_pair(color_code))


def draw_job_info(stdscr):
    y = Y_CLIENT+NUM_CLIENTS_IN_ROW+5
    x = TAB_SIZE
    stdscr.addstr(y, x, "=== job info ===")
    stdscr.addstr(y+2, x, "Number of remaining jobs: {:8d}".format(len(job_queue)))
    job_str_tpl = "{:%ds}"%TEXT_WIDTH
    job_str = job_str_tpl.format(get_next_job(job_queue))
    for i in range(4):
        stdscr.addstr(y+4+i, x, ' '*TEXT_WIDTH)
        stdscr.addstr(y+4+i, x, job_str[i*TEXT_WIDTH:(i+1)*TEXT_WIDTH])


def draw_screen(stdscr):
    stat = ScreenStat()
    init_screen(stdscr)
    draw_worker_list(stdscr)
    draw_job_info(stdscr)

    widx = 0
    #while (stat.k != ord('q')):
    while True:
        #stdscr.clear()
        ts = time.time_ns()
        stat.update(stdscr)
        draw_screen_core(stdscr, widx)

        stdscr.refresh()
        stat.k = stdscr.getch()
        t = time.time_ns() - ts
        time.sleep(max(0, (REFRESH_PERIOD_NS-t)/1e9))
        widx = (widx+1)%len(worker_list)
        if len(job_queue) == 0 and is_terminated_all(status_list): break

    stdscr.addstr(stat.y_max-2, TAB_SIZE, 'FINISHED: press any key ...')
    stdscr.refresh()
    input()


def draw_screen_core(stdscr, widx):
    worker = worker_list[widx]
    if status_list[widx] == 'WAIT':
        try: job = get_new_job(stdscr)
        except: pass
        else: 
            if is_skippable(job): logger.info(worker.host+" SKIP "+job)
            else: run_job(stdscr, job, worker, widx)
    else:
        update_status(widx, get_status(worker))
        if prev_status_list[widx] == 'RUN' and status_list[widx] == 'WAIT':
            logger.info(worker.host+" WAIT")
    draw_worker(stdscr, Y_CLIENT+1, TAB_SIZE, widx)


def get_new_job(stdscr):
    job = job_queue.popleft()
    while job in done_set:
        job = job_queue.popleft()
        draw_job_info(stdscr)
    return job


def is_skippable(job):
    dict_failed = get_failed_info()
    key, curr = parse_job_str(job)
    prev = dict_failed[key]
    #print(key, prev)
    assert type(curr) == tuple
    assert type(prev) == set
    return is_dominant_by(curr, prev)


def is_dominant_by(target, failed_list):
    for failed in failed_list:
        #print(target, failed, min(x[1]-x[0] for x in zip(failed, target)))
        if min(x[1]-x[0] for x in zip(failed, target)) >= 0: return True
    return False


def get_failed_info():
    l = list(map(lambda x:('_'.join(x[0:3]), (int(x[3]), int(x[4]), int(x[5]), float(x[6]), -float(x[2].split(',')[0].split(':')[1]))), map(lambda x:tuple(x.replace('Fopt_', 'Fopt-').split('_')), os.listdir('failed'))))
    d = defaultdict(set)
    for e in l: d[e[0]].add(e[1])
    return d


def parse_job_str(job):
    d = {}
    tokens = job.split('&&')[0].split(' ')
    for i in range(len(tokens)):
        if tokens[i].startswith('-'):
            d[tokens[i][1:]] = tokens[i+1]
    d['param'] = d['param'].replace('_', '-')
    d['theta'] = list(map(lambda x:x.split(':')[1], filter(lambda x:x.startswith('theta'), d['param'].split(','))))[0]
    return "{data}_{alg}_{param}".format(**d), (int(d['nt']), int(d['nr']), int(d['ql']), float(d['lr']), -float(d['theta']))


def run_job(stdscr, job, worker, widx):
    logger.info(worker.host+" RUN "+job)
    run_command(worker, job, False)
    draw_job_info(stdscr)
    update_status(widx, 'RUN')
    assigned_job_list[widx] = job
    add_to_done_set(assigned_job_list[widx])


def add_to_done_set(job):
    with open(inputname+'.done', 'a') as f:
        f.write(job+'\n')


def load_done_set():
    done_set = set([])
    if not os.path.exists(inputname+'.done'):
        return done_set
    with open(inputname+'.done') as f:
        for line in f: done_set.add(line.strip())
    return done_set


def init_env():
    for path in glob.glob('./failed/*'): os.remove(path)
    for path in glob.glob('./*.done'): os.remove(path)


#worker_blacklist = set(['cherry01', 'cherry02', 'cherry04', 'cherry11', 'cherry28'])
worker_blacklist = set([])

if __name__ == '__main__':
    #init_env()
    n_max_worker = 42
    worker_list = get_worker_list(n_max_worker)
    print(worker_list)
    print('Num workers:', len(worker_list))
    assert len(worker_list) == 37
    status_list = ["NONE"] * len(worker_list)
    prev_status_list = ["NONE"] * len(worker_list)
    assigned_job_list = [""] * len(worker_list)
    NUM_CLIENTS_IN_ROW, NUM_CLIENTS_IN_COL = get_size_of_worker_window()

    inputname = sys.argv[1]
    logger = getLogger(inputname)
    done_set = load_done_set()

    job_queue = build_job_queue(inputname)
    curses.wrapper(draw_screen)
    close_all(worker_list)
