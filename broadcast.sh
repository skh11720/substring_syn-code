src=$1
dst=$1
for num in {01..42}; do
	scp ${src} cherry${num}:~/ghsong/substring_syn/${dst}
done
