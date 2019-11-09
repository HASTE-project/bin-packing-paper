Ansible Playbooks for Automated Deployment of servers for benchmarking study

see: 
_Apache Spark Streaming and HarmonicIO: A Performance and Architecture Comparison_ [https://arxiv.org/abs/1807.07724]

Also used for Uppsala University LDSA Course 2019 --  [https://studentportalen.uu.se/portal/portal/uusp/student/student-courses?courseCode=1TD268]

Tested with Ubuntu LTS 16.04

Ensure that SSH configuration and IP addresses are configured (in `~/.ssh/config` and `/etc/hosts`) first. See 'hostnames.yml' for more details.

For HPC2N use -i hosts_hpc2n
For UPPMAX use -i hosts_uppmax

**Note that many hosts do not have public IPs, you will need to configure SSH forwarding via one of the servers with a public IP**

```
ansible -i hosts_uppmax_ldsa all -a "echo hi"
ansible -i hosts_hpc2n all -a "echo hi"
```

To deploy entire pipeline (dry run):

```
ansible-playbook -i hosts_hpc2n site.yml --check
ansible-playbook -i hosts_uppmax site.yml --check
```

To deploy for real:
```
ansible-playbook -i hosts_hpc2n site.yml
ansible-playbook -i hosts_uppmax_ldsa site.yml
```

To restart the Spark master and slaves:
(factored out to allow easy restarting for benchmarking tests)

GOTCHA: check the Spark master host name - its hard coded!!!


ansible -i hosts_uppmax_ldsa all -a "echo hi"
ansible -i hosts_uppmax_ldsa all -a "python3 --version"
ansible -i hosts_uppmax_ldsa all -a "sudo apt update"


```
ansible-playbook -i hosts_ben_uppmax playbooks-util/restart-spark-cluster.yml
```

Contributors: Ben Blamey
