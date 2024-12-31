
Voraussetzung:
* Hetzner Projekt muss erstellt worden sein
* API-Token für das Projekt generiert werden sein. Der API Token 
entscheidet zu welchem Projekt die Server hinzugefügt werden.
* SSH_Key für die Administration von Ansible mit den Server 

### SSH Key erstellen
Details unter https://community.hetzner.com/tutorials/howto-ssh-key
In lokaler GIT-Bash:
```
ssh-keygen -t ed25519
```
Den Public-Key als `opensbpm@hetzner` in den SSH-Keys des Hetzner-Projekts hinterlegen.
Bei wsl den key nach `~/.ssh/opensbpm-hetzner` kopieren

## Requirements
Install hetzner.hcloud and kubernetes.core with ansbile-galaxy.
```
ansible-galaxy collection install hetzner.hcloud
ansible-galaxy collection install kubernetes.core
```

### Run in wsl
API-Token muss als Environment-Variable gesetzt worden sein
```
export HCLOUD_TOKEN=<Hetzner API Token>
```

```
ansible-playbook playbooks/setup_cluster.yml
```

### Run in powershell
To run playbook in powershell (standalone) run (in root dir):
```
docker run --rm `
    --mount type=bind,source="$(pwd)",target=/srv/ansible `
    --mount type=bind,source="$HOME"/.ssh,target=/tmp/_ssh `
    -w /srv/ansible `
    sedstef/ansible-playbook:latest
     
```

### Check
Check kubeconfig with kubectl
```
kubectl --kubeconfig ~/.kube/opensbm\@hetzner cluster-info
```

== TODO
https://docs.ansible.com/ansible/2.8/user_guide/playbooks_best_practices.html
