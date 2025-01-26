# OpenSBPM

OpenSBPM is a distributed application, based on the Spring Boot Framework, designed to manage and execute subject oriented 
business process management (SBPM) workflows. This project includes tools for setting up a microservices architecture with 
Kubernetes and testing system reliability.

## Features
- **Distributed Spring Boot Application**: Manages complex SBPM workflows.
- **Ansible Playbook**: Automates the creation of a MicroK8s cluster on Hetzner.
- **Userbot**: Runs stress and chaos tests for ensuring application resilience.

## Installation

### Prerequisites
- Ansible installed on your local machine
- MicroK8s installed or a Hetzner account for cloud provisioning

### Setup
1. Clone this repository:  
   `git clone https://github.com/opensbpm/opensbpm.git`

2. Check requirements defined in [ansible requirements](ansible/README.md#requirements)
   
3. Run ansible playbook to create a MicroK8s cluster 
```
export HCLOUD_TOKEN=<Hetzner API Token>
ansible-playbook ansible/site.yml
```

More details in [ansible README](ansible/README.md)
   

## Testing

The repository includes a Userbot to perform stress and chaos testing to simulate various failure conditions and ensure 
system stability.

### Run Tests
Execute the Userbot with the desired test parameters to start stress testing:
```bash
./userbot.sh --type stress --duration 60
```
