from collections import defaultdict
import itertools
import random as rn
import yaml


dict_data = yaml.load(open('data_info.yml'), Loader=yaml.FullLoader)
dict_alg = yaml.load(open('alg_info.yml'), Loader=yaml.FullLoader)

#alg_list = ['PrefixSearch']
#alg_list = ['ZeroPrefixSearch']
#alg_list = ['PkwiseSynSearch']
alg_list = [
		'PrefixSearch',
#		'PkwiseSynSearch',
#		'FaerieSynSearch',
#		'NaiveContainmentSearch',
#		'ContainmentPrefixSearch',
		]
alg_order = {
		'PrefixSearch':0, 
		'PkwiseSynSearch':1, 
		'FaerieSynSearch':2,
		'NaiveContainmentSearch':3,
		'ContainmentPrefixSearch':4,
}

data_list = [
		'WIKI',
		'PUBMED',
		'AMAZON',
		'WIKI-DOC',
		'PUBMED-DOC',
		'AMAZON-DOC',
		]
data_order = {'WIKI':0, 'PUBMED':1, 'AMAZON':2, 'WIKI-LONG':3, 'PUBMED-LONG':4, 'AMAZON-LONG':5, 'WIKI-DOC':6, 'PUBMED-DOC':7, 'AMAZON-DOC':8}

task_tmpl = 'timeout -k 1 28800 java -Xmx8g -cp target/substring-syn-0.0.1-SNAPSHOT.jar snu.kdd.substring_syn.App -data {data} -nt {nt} -nr {nr} -ql {ql} -lr {lr} -nar {nar} -alg {alg} -param {param} && bash ./upload.sh || (touch MSG && scp -q MSG cherry-manage:/home/hadoop/ghsong/substring_syn/failed/{data}_{alg}_{param}_{nt}_{nr}_{ql}_{lr}_{nar} && rm MSG)'


nt_max = '1000000'
nt0 = '100000'
ql0 = '5'
lr0 = '1.0'



def add_data_setting_all(data, nt_list_=None, ql_list_=None, nr_list_=None, lr_list_=None, nar_list_=None, opt=None):
	nt_list = dict_data[data]['nt'] if nt_list_ is None else nt_list_
	ql_list = dict_data[data]['ql'] if ql_list_ is None else ql_list_
	nr_list = dict_data[data]['nr'] if nr_list_ is None else nr_list_
	lr_list = dict_data[data]['lr'] if lr_list_ is None else lr_list_
	nar_list = dict_data[data]['nar'] if nar_list_ is None else nar_list_

	def iter_vary_nt():
		for nt in nt_list:
			if int(nt) <= int(nt_max):
				yield nt, ql0, nr_list[-1], lr0, -1
	
	def iter_vary_ql():
		for ql in ql_list:
			yield nt0, ql, nr_list[-1], lr0, -1
	
	def iter_vary_nr():
		for nr in nr_list:
			yield nt0, ql0, nr, lr0, -1
	
	def iter_vary_lr():
		for lr in lr_list:
			yield nt0, ql0, nr_list[-1], lr, -1
	
	def iter_vary_theta():
		yield nt0, ql0, nr_list[-1], lr0, -1

	def iter_vary_nar():
		for nt in nt_list:
			for nar in nar_list:
				yield nt, ql0, nr_list[-1], lr0, nar

	if opt is None: add_exp_all(data, [
		iter_vary_nt(), 
		iter_vary_ql(), 
		iter_vary_nr(), 
		iter_vary_lr()
		])
	elif opt == 'nt': add_exp_all(data, [iter_vary_nt()])
	elif opt == 'ql': add_exp_all(data, [iter_vary_ql()])
	elif opt == 'nr': add_exp_all(data, [iter_vary_nr()])
	elif opt == 'lr': add_exp_all(data, [iter_vary_lr()])
	elif opt == 'nar': add_exp_all(data, [iter_vary_nar()])
	elif opt == 'theta': add_exp_all(data, [iter_vary_theta()])
	else: print(opt)


def add_exp_all(data, exp_list):
	set_viewed = set([])
	for opt in itertools.chain(*exp_list):
		if opt in set_viewed: continue
		else:
			set_viewed.add(opt)
			for alg in alg_list:
				add_exp(data, *opt, alg)


def add_exp(data, nt, ql, nr, lr, nar, alg):
	key_list, val_list = map(list, zip(*dict_alg[alg].items()))
	for vals in itertools.product(*val_list):
		exp_list.append({'data':data, 'alg':alg, 'nt':nt, 'ql':ql, 'nr':nr, 'lr':lr, 'nar':nar, **dict(zip(key_list,vals))})


def sort_exp_list(exp_list):
	return list(sorted(exp_list, key=lambda x:(
		int(x['nar']),
		int(x['nt']),
		int(x['ql']),
		-float(x['theta']),
		int(x['nr']),
		float(x['lr']),
		alg_order[x['alg']],
		filter_order[x['filter']] if x['alg'].startswith('PrefixSearch') else 1,
		data_order[x['data']],
		)))


def output_exp_list(exp_list):
	with open('exp_list.txt', 'w') as f:
		for exp in exp_list:
			key_list = dict_alg[exp['alg']].keys()
			param = ','.join([k+':'+exp[k] for k in key_list])
			exp['param'] = param
			#t_sleep = rn.random()*5
			t_sleep = 1
			task = task_tmpl.format(**exp)
			f.write(task+'\n')


filter_order = {'Fopt_None':13, 'Fopt_Index':12, 'Fopt_C':11, 'Fopt_P':10, 'Fopt_L':9, 'Fopt_R':8, 'Fopt_IL':7, 'Fopt_IR':6, 'Fopt_CP':5, 'Fopt_CL':4, 'Fopt_PL':3, 'Fopt_CPL':2, 'Fopt_CPLR':1}
exp_list = []
for data in data_list: add_data_setting_all(data, opt='nar')
exp_list = sort_exp_list(exp_list)
output_exp_list(exp_list)


