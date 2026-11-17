# Bingo Portfolio Project — Cloud Deployment Plan

## Project Overview

I'm building a multiplayer Bingo application as a portfolio project to demonstrate and strengthen my Java backend, Spring Boot, Kafka, and React experience.

The project is organized into separate Git repositories:

- `bingo-backend` — Java / Spring Boot backend, game logic, REST APIs, Kafka integration, and WebSocket communication.
- `bingo-frontend` — React frontend for players.
- `bingo-lancebot` — Separate Java application that acts as an automated Bingo player. It will eventually have an optional JavaFX UI.
- `bingo-infrastructure` — Infrastructure-as-code configurations for cloud deployments.

The backend owns the authoritative game state and rules. Kafka handles backend events, and the backend distributes relevant events to connected clients over WebSockets. Neither the browser nor the bot connects directly to Kafka.

## Cloud Strategy: Evaluate Azure and OCI, Then Deploy to AWS

The goal is to get a working application deployed affordably, then use the project as an opportunity to gain practical experience with major cloud platforms.

Rather than committing immediately to OCI, evaluate Azure first. I've never used an Azure free trial, and Azure is a major enterprise cloud platform that would be valuable to learn.

### 1. Initial Cloud Deployment — Azure or OCI

Evaluate the free and low-cost options available through both platforms.

**Azure is worth investigating first:**

- Azure free account offers an introductory credit for eligible new customers.
- Azure Container Apps could host the Spring Boot backend.
- Azure Static Web Apps could host the React frontend.
- A small Linux VM could host a self-managed Kafka broker.
- Investigate networking, persistent storage, and ongoing costs.

**OCI remains a good alternative:**

- Oracle Cloud Infrastructure offers an Always Free tier with VM resources.
- A Spring Boot backend and self-managed Kafka broker could initially run on the same VM.
- React frontend could be hosted through Cloudflare Pages.
- This is a practical option for an affordable, persistent public demo.

The priority is getting the application working reliably, not building an elaborate production infrastructure.

### 2. AWS — Temporary Hands-On Deployment

After the application is working, deploy it to AWS to gain practical, demonstrable AWS experience.

This is intended to be a temporary learning exercise, not necessarily the permanent hosting environment.

Proposed AWS architecture:

- **ECS / Fargate:** Run the Spring Boot backend.
- **ECR:** Store container images.
- **EC2:** Host a self-managed Kafka broker to avoid the expense of Amazon MSK.
- **VPC:** Network isolation and connectivity.
- **Security Groups:** Control traffic between the load balancer, backend, and Kafka.
- **Application Load Balancer:** Route API and WebSocket traffic.
- **ACM:** Manage HTTPS certificates.
- **CloudWatch:** Logging and monitoring.
- **IAM:** Service roles and permissions.
- **GitHub Actions:** CI/CD pipelines.
- **Terraform or AWS CDK:** Infrastructure as code.

I have previously worked with AWS services and concepts including ECS/Fargate, VPCs, security groups, and GitHub Actions. ACM is less familiar, and infrastructure as code would be a useful area to develop further.

### AWS Cost Strategy

Amazon MSK Serverless is too expensive for this small project. A self-managed Kafka broker on a small EC2 instance is the preferred approach.

The AWS deployment should be temporary:

1. Provision the infrastructure using infrastructure as code.
2. Deploy the application and configure the services.
3. Work through networking, security, HTTPS, logging, and CI/CD.
4. Verify that the application works.
5. Document the architecture and deployment process.
6. Tear down the AWS resources to stop ongoing charges.

Be particularly careful about resources that can continue generating charges, including EC2, EBS volumes, load balancers, NAT Gateways, public IPv4 addresses, and data transfer. Verify resource deletion and billing after teardown.

The goal is to gain real AWS deployment experience without paying to host the application there indefinitely.

## Infrastructure as Code

Prefer Terraform for this project, although AWS CDK is also an option.

Avoid building the infrastructure entirely by hand. Manual configuration is error-prone and difficult to reproduce.

Keep the infrastructure definitions in the `bingo-infrastructure` repository, with separate Azure, OCI, and AWS configurations as appropriate.

## Portfolio and Resume Value

The project should provide demonstrable experience with:

- Java and Spring Boot backend development.
- Kafka event-driven architecture.
- REST APIs and WebSockets.
- React frontend development.
- Docker and container deployment.
- AWS ECS/Fargate, ECR, EC2, VPC, security groups, ALB, IAM, ACM, and CloudWatch.
- GitHub Actions CI/CD.
- Terraform / infrastructure as code.
- Potentially Azure Container Apps, Static Web Apps, and Azure networking.
- Deploying the same application across different cloud environments.

Be accurate when describing the experience. These will be hands-on portfolio deployments, not operating a large-scale production system.

## Guiding Principles

- Build the application first.
- Evaluate Azure's free trial and OCI's Always Free offerings before choosing the initial hosting environment.
- Use AWS as a temporary learning environment to gain practical cloud deployment experience.
- Keep infrastructure definitions reproducible and version-controlled.
- Avoid unnecessary complexity and ongoing cloud costs.
- Document what was actually implemented and learned.
