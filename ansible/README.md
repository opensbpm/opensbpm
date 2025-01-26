# OpenSBPM - Ansible Playbook

This playbook handles all aspects of managing a MicroK8s cluster on Hetzner Cloud. It provisions a control plane and multiple 
worker nodes, sets up a MicroK8s cluster, and deploys OpenSBPM as a distributed, cloud-native application.


## Requirements
Install _hetzner.hcloud_ and _kubernetes.core_ with ansible-galaxy.

```
ansible-galaxy collection install hetzner.hcloud
ansible-galaxy collection install kubernetes.core
```

## Prepare Hetzner Cloud

- Ensure a Hetzner project is available.
- Generate an API token for the project to enable automation.
- Set up an SSH key for Ansible administration on the servers.


### Create SSH Key

Generate a local SSH key, which will be registered in Hetzner Cloud. This key will be used by all servers to enable secure 
root access.

```
ssh-keygen -t ed25519 -f ~/.ssh/opensbpm@hetzner
```

For details see https://community.hetzner.com/tutorials/howto-ssh-key


## Setup microk8s cluster

### Run ansible playbook 
Export Hetzner API Token as HCLOUD_TOKEN und execute ansible playbook.
```
export HCLOUD_TOKEN=<Hetzner API Token>
ansible-playbook ansible/site.yml
```

### Run in powershell with docker

**Use with caution**
To run playbook in powershell (standalone) run (in root dir):
```
docker run --rm `
    --mount type=bind,source="$(pwd)",target=/srv/ansible `
    --mount type=bind,source="$HOME"/.ssh,target=/tmp/_ssh `
    -w /srv/ansible `
    sedstef/ansible-playbook:latest
     
```

## Check cluster
Check kubeconfig with kubectl
```
kubectl --kubeconfig ~/.kube/opensbpm\@hetzner.yaml cluster-info
```

To show the latest events, use:
```
kubectl --kubeconfig ~/.kube/opensbpm\@hetzner.yaml events
```

### Login in control-plane
```
ssh-keygen -R cloud-dev.opensbpm.org; ssh -i ~/.ssh/opensbpm\@hetzner root@cloud-dev.opensbpm.org
```

## Delete Hetzner Cloud
```
ansible-playbook ansible/delete_site.yml
```


## TODO
https://docs.ansible.com/ansible/2.8/user_guide/playbooks_best_practices.html

