# Lab 9 - Controlling Access to Resources in Kubernetes
---


### Lab Steps
- [Step 1. Understanding Kubernetes RBAC model](#step-1-understanding-kubernetes-rbac-model)
- [Step 2. RBAC: Create user with limited namespace access](#step-2-rbac-create-user-with-limited-namespace-access)
  - [Task 1. Create the office namespace](#task-1-create-the-office-namespace)
  - [Task 2. Create the user credentials](#task-2-create-the-user-credentials)
  - [Task 3: Create the role for managing deployments](#task-3-create-the-role-for-managing-deployments)
  - [Task 4: Bind the role to the maxime user](#task-4-bind-the-role-to-the-maxime-user)
- [Step 3. Service Accounts](#step-3-service-accounts)
  - [The Default Service Account](#the-default-service-account)
  - [ServiceAccounts and RBAC](#serviceaccounts-and-rbac)
- [Step 4. Configuring a Security Context for a Pod or Container](#step-4-configuring-a-security-context-for-a-pod-or-container)
  - [Set the security context for a Pod](#set-the-security-context-for-a-pod)
  - [Set the security context for a Container](#set-the-security-context-for-a-container)
- [Step 5. Implementing a Network Policy](#step-5-implementing-a-network-policy)
  - [LIMIT traffic to an application](#limit-traffic-to-an-application)
    - [Example](#example)
    - [Try it out](#try-it-out)
    - [Cleanup](#cleanup)

<div style="page-break-after: always"></div>


This lab will go through the basic Kubernetes Role-Based Access Control (RBAC) API Objects, together with a common use cases : create a user with limited access. RBAC policies are vital for the correct management of your cluster, as they allow you to specify which types of actions are permitted depending on the user and their role in your organization. Examples include:

- Secure your cluster by granting privileged operations (accessing secrets, for example) only to admin users.
- Force user authentication in your cluster.
- Limit resource creation (such as pods, persistent volumes, deployments) to specific namespaces. You can also use quotas to ensure that resource usage is limited and under control.
- Have a user only see resources in their authorized namespace. This allows you to isolate resources within your organization (for example, between departments).

# Step 1. Understanding Kubernetes RBAC model
In order to fully grasp the idea of RBAC, we must understand that three elements are involved:

- **Subjects**: The set of users and processes that want to access the Kubernetes API. These correspond to the entity that attempts an operation in the cluster. There are three types of subjects:
   - User Accounts: These are global, and meant for humans or processes living outside the cluster. There is no associated resource API Object in the Kubernetes cluster.
   - Service Accounts: This kind of account is namespaced and meant for intra-cluster processes running inside pods, which want to authenticate against the API.
   - Groups: This is used for referring to multiple accounts. There are some groups created by default such as cluster-admin (explained in later sections).
- **Resources**: The set of Kubernetes API Objects available in the cluster. Examples include Pods, Deployments, Services, Nodes, and PersistentVolumes, among others. At a higher level, resources are associated with [API Groups](https://kubernetes.io/docs/concepts/overview/kubernetes-api/#api-groups) (for example, Pods belong to the core API group whereas Deployments belong to the apps API group). 

  One basic Kubernetes feature is that all its resources are modeled API objects, which allow CRUD (Create, Read, Update, Delete) operations.
- **Verbs**: The set of operations that can be executed to the resources above. Different verbs are available (examples: `get`, `create`, `delete`,`watch`, `list`, etc.). 
 
With these three elements in mind, the key idea of RBAC is the following:

   _We want to connect subjects, API resources, and operations. In other words, we want to specify, given a user, which operations can be executed over a set of resources._

The [figure below](https://github.com/reselbob/k8sassets/blob/master/rbac/images/RBAC-items.jpeg) [Credit to Bob Reseman] illustrates the RBAC model.
 ![figure](images/RBAC-items.jpeg)

# Step 2. RBAC: Create user with limited namespace access
In this example, we will create the following User Account:

- Username: `maxime`
- Group: `businesstraining`

We will add the necessary RBAC policies so this user can fully manage deployments (i.e. use kubectl run command) only inside the `office` namespace. At the end, we will test the policies to make sure they work as expected.

## Task 1. Create the office namespace
Execute the kubectl create command to create the namespace (as the admin user):

```shell
kubectl create namespace office
```
## Task 2. Create the user credentials
Kubernetes does not have API Objects for User Accounts. Of the available ways to manage authentication (see Kubernetes official documentation for a complete list, we will use OpenSSL certificates for their simplicity. The necessary steps are:

- Create a private key for your user. In this example, we will name the file `maxime.key`:

  ```shell
  openssl genrsa -out maxime.key 2048
  ```

- Create a certificate sign request `maxime.csr` using the private key you just created (`maxime.key` in this example). Make sure you specify your username and group in the -subj section (CN is for the username and O for the group). As previously mentioned, we will use `maxime` as the name and `businesstraining` as the group:

  ```shell
  openssl req -new -key maxime.key -out maxime.csr -subj "/CN=maxime/O=businesstraining"
  ```

- Locate your Kubernetes cluster certificate authority (CA). This will be responsible for approving the request and generating the necessary certificate to access the cluster API. Its location in the case of Minikube is `~/.minikube/`. In the case of Docker Desktop Kubernetes, they are not mounted locally. Use the following `kubectl cp` command to get the certificate:

  ```shell
  kubectl cp kube-apiserver-docker-desktop:run/config/pki/ca.crt -n kube-system /tmp/ca.crt
  ```
- Generate the final certificate `maxime.crt` by approving the certificate sign request, `maxime.csr`, you made earlier. Make sure you substitute the CA_LOCATION placeholder with the location of your cluster CA. In this example, the certificate will be valid for 500 days:

  ```shell
  openssl x509 -req -in maxime.csr -CA CA_LOCATION/ca.crt -CAkey CA_LOCATION/ca.key -CAcreateserial -out maxime.crt -days 500
  # In case of Minikube change CA_LOCATION with $HOME/.minikube
  ```
- Save both `maxime.crt` and `maxime.key` in a safe location (in this example we will create a  folder named `.certs/`).

- Add a new context with the new credentials for your Kubernetes cluster. This example is for a Docker Desktop cluster but it should be similar for others:

  ```shell
  kubectl config set-credentials maxime --client-certificate=.certs/maxime.crt  --client-key=.certs/maxime.key
  kubectl config set-context maxime-context --cluster=docker-desktop --namespace=office --user=maxime
  ```
Now you should get an access denied error when using the kubectl CLI with this configuration file. This is expected as we have not defined any permitted operations for this user.

```shell
kubectl --context=maxime-context get pods
```

## Task 3: Create the role for managing deployments

- Create a `role-deployment-manager.yaml` file with the content below. In this yaml file we are creating the rule that allows a user to execute several operations on Deployments, Pods and ReplicaSets (necessary for creating a Deployment), which belong to the core (expressed by "" in the yaml file), apps, and extensions API Groups:

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  namespace: office
  name: deployment-manager
rules:
- apiGroups: ["", "extensions", "apps"]
  resources: ["deployments", "replicasets", "pods"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"] # You can also use ["*"]
```

- Create the Role in the cluster using the `kubectl create` role command:
  
```shell
kubectl create -f role-deployment-manager.yaml
```

## Task 4: Bind the role to the maxime user

- Create a `rolebinding-deployment-manager.yaml` file with the content below. In this file, we are binding the deployment-manager Role to the User Account maxime inside the office namespace:

```yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: deployment-manager-binding
  namespace: office
subjects:
- kind: User
  name: maxime
  apiGroup: ""
roleRef:
  kind: Role
  name: deployment-manager
  apiGroup: ""

```

- Deploy the RoleBinding by running the kubectl create command:

  ```shell
  kubectl create -f rolebinding-deployment-manager.yaml
  ```
Step 5: Test the RBAC rule
Now you should be able to execute the following commands without any issues:

```bash
kubectl --context=maxime-context run --image bitnami/dokuwiki mydokuwiki
kubectl --context=maxime-context get pods
```
If you run the same command with the --namespace=default argument, it will fail, as the maxime user does not have access to this namespace.

```bash
kubectl --context=maxime-context get pods --namespace=default
```
Now you have created a user with limited permissions in your cluster.

To list all contexts:

```shell
kubectl config get-contexts
```

To set the `maxime-context` as the default context without having to specify the `--context=maxime-context` :

```shell
kubectl config use-context maxime-context
```
You can now view the pods as `maxime` in the namespace `office`, using the following command:
```shell
kubectl get pods
```

# Step 3. Service Accounts
In the Kubernetes cluster, any processes or applications in the container which resides within the pod can access the cluster by getting authenticated by the API server, using a service account. For Example: An application like Prometheus accessing the cluster to monitor it is a type of service account.

A service account is an identity that is attached to the processes running within a pod.

Unlike user accounts, the service account is namespaced so you can create multiple service accounts with the same name as long as they live in different namespaces.

## The Default Service Account

By default, the service account credentials are mounted to the pods through a secret. Let's have a quick example:

```shell
$ kubectl run -it --rm testpod --restart=Never --image=alpine -- sh
If you don't see a command prompt, try pressing enter.
/ # ls -l /var/run/secrets/kubernetes.io/serviceaccount/
total 0
lrwxrwxrwx    1 root     root            13 Nov  8 11:45 ca.crt -> ..data/ca.crt
lrwxrwxrwx    1 root     root            16 Nov  8 11:45 namespace -> ..data/namespace
lrwxrwxrwx    1 root     root            12 Nov  8 11:45 token -> ..data/token
```

This is a one-time pod that hosts an Alpine image. Opening a shell to the container, we could easily view the files that were mounted to the pod from an outside secret under the `/data` mount point. The authentication process is done through the token file. The token is signed by the master key of the Kubernetes cluster. Let’s have a look at the contents of this file:

```shell
/ # cat /var/run/secrets/kubernetes.io/serviceaccount/token && echo
eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tcHh6ZjciLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImUzMzFmNTU1LTAxOTktMTFlYS1hYTVhLTQyMDEwYTgwMDBhYSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.I3jFqRd7ANj4BidzoW-v_YO1N_z2p33Ex1biaz_J97QK9CRlIFmK2KC1k0RbQux3zo72xsLhbPjNH40pBzqr2TzVjZKFD7T8-ihDj1Og5L_BAud-CtT4e-0zcvC5rTKcWzkUn5a64TUSwF5Q6I3KjeaE3pmDFwG4I6XLuODCVOwRkfn0V_LzjqLy3nXfUK8FpIkeBrRd9QN68PG9YFH9lKMgGZLvuG_m6K6EFxqBpVMec8SPsG77GGRezC9Mjsyxp2Wie-j8vUVb5et2o4ShKa8sp-Nqum4bpBxGQ9NBo3qlefuqYGYivGZBnL8dienBLFbO5swSHUL6vxTgaRg2kA
/ #
```
The contents of the token is of the JSON Web Token (JWT) format. It can be decoded using the jwt command-line tool or online on the jwt.io website. The following is the output of the decoded operation on the web likewise on the command-line:

![jwtio](images/jwt-io.png)

The interesting part of the output is the Payload. As you can see, we have the service-account.name set to `default`. This is the default service account that gets created and mounted automatically to every pod upon initialization. In most cases, you shouldn’t use the default account when you need to give your pod more privileges. Let’s create a service account and assign it to a pod:

```shell
$ kubectl create serviceaccount testsa
serviceaccount/testsa created
```
Notice that we did not specify a namespace here so our testsa service account is created in the default namespace. If we take a look at the Secrets in this namespace we shall find two secrets:

```shell
$ kubectl get secrets
NAME                  TYPE                                  DATA   AGE
default-token-pxzf7   kubernetes.io/service-account-token   3      16h
testsa-token-zwhvm    kubernetes.io/service-account-token   3      11s
```

The first secret is the one created for the default service account that we examined earlier. The second one holds the credentials for the testsa account that we’ve just created.

So, now that we have our service account ready, let’s attach it to a pod. The following definition creates a pod that uses the testsa as a service account:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: testpod
spec:
  serviceAccountName: testsa
  containers:
    - name: mycontainer
      image: alpine:latest
      command:
        - "sh"
        - "-c"
        - "sleep 1000"
```
The interesting part in this definition is where we specify the serviceAccountName parameter. The container itself runs the Alpine image and we instruct it to sleep for 1000 seconds, enough for us to examine the container contents before the process exits. Now, if you apply this definition to your cluster, you can log in to the pod using a command like the following:

```shell
kubectl exec -it testpod -- sh
```
If you examine the contents of `/var/run/secrets/kubernetes.io/serviceaccount/` you would have a token file but (when decrypted using the jwt tool) contains different service account name and credentials.


## ServiceAccounts and RBAC

For each Pod that has no ServiceAccount specified the default ServiceAccount is attached and its default token is mounted.

Let's run a Pod:

```shell
$kubectl run -i --tty --rm ca-test-pod --image=radial/busyboxplus:curl
kubectl run --generator=deployment/apps.v1 is DEPRECATED and will be removed in a future version. Use kubectl run --generator=run-pod/v1 or kubectl create instead.
If you don't see a command prompt, try pressing enter.
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$
```

Check its `volumeMounts`, `serviceAccount`, and `volumes`:

```shell
$ kubectl get pod ca-test-pod-5c96c78d7f-wqlsq -o yaml
apiVersion: v1
kind: Pod
...
volumeMounts:
- mountPath: /var/run/secrets/kubernetes.io/serviceaccount
name: default-token-292g9
readOnly: true
...
serviceAccount: default
serviceAccountName: default
...
volumes:
- name: default-token-292g9
secret:
defaultMode: 420
secretName: default-token-292g
```

Inside of the pod check the `/var/run/secrets/kubernetes.io/serviceaccount` directory content:

```shell
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ ls -l /var/run/secrets/kubernetes.io/serviceaccount
ca.crt
namespace
token
```

And recall the content of the data section of the `default-token-292g9 `Secret:

```shell
$ kubectl get secret default-token-292g9 -o yaml
apiVersion: v1
data:
ca.crt: LS0t[...]
namespace: ZGVmYXVsdA==
token: ZXlKaGJ
...
```

Try to perform a request to the API-server without authentification – use the special Service kubernetes, add the `-k` or `--insecure` to the `curl` to skip server’s certificate validation

```shell
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ curl -k https://kubernetes
{
"kind": "Status",
"apiVersion": "v1",
"metadata": {
},
"status": "Failure",
"message": "forbidden: User \"system:anonymous\" cannot get path \"/\"",
"reason": "Forbidden",
"details": {
},
"code": 403
}
```
Right. We got the 403, Forbidden.

Now, add two variables – one with the `ca.crt` and with the token:

```shell
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ CERT=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
```

And run curl again – let’s try to get a list of the pods in our namespace, this time without --insecure and with authorization by using the Authorization header:

```shell
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ curl --cacert $CERT -H "Authorization: Bearer $TOKEN" "https://kubernetes/api/v1/namespaces/default/pods/"
{
"kind": "Status",
"apiVersion": "v1",
"metadata": {
},
"status": "Failure",
"message": "pods is forbidden: User \"system:serviceaccount:default:default\" cannot list resource \"pods\" in API group \"\" in the namespace \"default\"",
"reason": "Forbidden",
"details": {
"kind": "pods"
},
"code": 403
}
```

At this time, we are able to see our user – the User `"system:serviceaccount:default:default"`, but it has no permissions to perform requests as by default all users and ServiceAccounts have no privileges (the principle of the least privileges).

RoleBindig for ServiceAccount
To give our SericeAccount permissions we need to create a RoleBinding or ClusterRoleBinding as for normal users.

Create a RoleBinding mapping to the default ClusterRole `view`, see [User-facing roles](https://kubernetes.io/docs/reference/access-authn-authz/rbac/#user-facing-roles):

```shell
kubectl create rolebinding ca-test-view --clusterrole=view --serviceaccount=default:default
rolebinding.rbac.authorization.k8s.io/ca-test-view created
```

And run curl again:

```shell
[ root@ca-test-pod-5c96c78d7f-wqlsq:/ ]$ curl --cacert $CERT -H "Authorization: Bearer $TOKEN" "https://kubernetes/api/v1/namespaces/default/pods/"
{
"kind": "PodList",
"apiVersion": "v1",
"metadata": {
"selfLink": "/api/v1/namespaces/default/pods/",
"resourceVersion": "66892356"
},
"items": [
{
"metadata": {
"name": "ca-test-pod-5c96c78d7f-wqlsq",
"generateName": "ca-test-pod-5c96c78d7f-",
"namespace": "default",
"selfLink": "/api/v1/namespaces/default/pods/ca-test-pod-5c96c78d7f-wqlsq",
"uid": "f0d77cfe-38ab-48e9-aaf3-f344f1d343f3",
"resourceVersion": "66888089",
"creationTimestamp": "2020-11-17T16:08:09Z",
"labels": {
"pod-template-hash": "5c96c78d7f",
"run": "ca-test-pod"
},
...
"qosClass": "BestEffort"
}
}
]

```

# Step 4. Configuring a Security Context for a Pod or Container

A security context defines privilege and access control settings for a Pod or Container. Security context settings include, but are not limited to:

Discretionary Access Control: Permission to access an object, like a file, is based on [user ID (UID) and group ID (GID)](https://wiki.archlinux.org/index.php/users_and_groups).

  - [Security Enhanced Linux (SELinux)](https://en.wikipedia.org/wiki/Security-Enhanced_Linux): Objects are assigned security labels.
  - Running as privileged or unprivileged.
  - [Linux Capabilities](https://linux-audit.com/linux-capabilities-hardening-linux-binaries-by-removing-setuid/): Give a process some privileges, but not all the privileges of the root user.
  - [AppArmor](https://kubernetes.io/docs/tutorials/clusters/apparmor/): Use program profiles to restrict the capabilities of individual programs.
  - [Seccomp](https://en.wikipedia.org/wiki/Seccomp): Filter a process's system calls.
  - AllowPrivilegeEscalation: Controls whether a process can gain more privileges than its parent process. This bool directly controls whether the no_new_privs flag gets set on the container process. AllowPrivilegeEscalation is true always when the container is: 1) run as Privileged OR 2) has CAP_SYS_ADMIN.
  - readOnlyRootFilesystem: Mounts the container's root filesystem as read-only.

The above bullets are not a complete set of security context settings -- please see [SecurityContext](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.21/#securitycontext-v1-core) for a comprehensive list.

## Set the security context for a Pod

To specify security settings for a Pod, include the securityContext field in the Pod specification. The securityContext field is a PodSecurityContext object. The security settings that you specify for a Pod apply to all Containers in the Pod. Here is a configuration file for a Pod that has a securityContext and an emptyDir volume:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: security-context-demo
spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
  volumes:
  - name: sec-ctx-vol
    emptyDir: {}
  containers:
  - name: sec-ctx-demo
    image: busybox
    command: [ "sh", "-c", "sleep 1h" ]
    volumeMounts:
    - name: sec-ctx-vol
      mountPath: /data/demo
    securityContext:
      allowPrivilegeEscalation: false
```
In the configuration file, the runAsUser field specifies that for any Containers in the Pod, all processes run with user ID 1000. The runAsGroup field specifies the primary group ID of 3000 for all processes within any containers of the Pod. If this field is omitted, the primary group ID of the containers will be root(0). Any files created will also be owned by user 1000 and group 3000 when runAsGroup is specified. Since fsGroup field is specified, all processes of the container are also part of the supplementary group ID 2000. The owner for volume /data/demo and any files created in that volume will be Group ID 2000.

Create the Pod:

```shell
kubectl apply -f https://k8s.io/examples/pods/security/security-context.yaml
```
Verify that the Pod's Container is running:

```shell
kubectl get pod security-context-demo
```
Get a shell to the running Container:

```shell
kubectl exec -it security-context-demo -- sh
```
In your shell, list the running processes:

```shell
ps
```
The output shows that the processes are running as user 1000, which is the value of runAsUser:

```shell
PID   USER     TIME  COMMAND
    1 1000      0:00 sleep 1h
    6 1000      0:00 sh
...
```

In your shell, navigate to /data, and list the one directory:

```shell
cd /data
ls -l
```
The output shows that the /data/demo directory has group ID 2000, which is the value of fsGroup.

```shell
drwxrwsrwx 2 root 2000 4096 Jun  6 20:08 demo
```
In your shell, navigate to /data/demo, and create a file:

```shell
cd demo
echo hello > testfile
```

List the file in the /data/demo directory:

```shell
ls -l
```
The output shows that testfile has group ID 2000, which is the value of fsGroup.

```shell
-rw-r--r-- 1 1000 2000 6 Jun  6 20:08 testfile
```
Run the following command:

```shell
$ id
uid=1000 gid=3000 groups=2000
```
You will see that gid is 3000 which is same as runAsGroup field. If the runAsGroup was omitted the gid would remain as 0(root) and the process will be able to interact with files that are owned by root(0) group and that have the required group permissions for root(0) group.

Exit your shell:

```shell
exit
```

## Set the security context for a Container

To specify security settings for a Container, include the securityContext field in the Container manifest. The securityContext field is a SecurityContext object. Security settings that you specify for a Container apply only to the individual Container, and they override settings made at the Pod level when there is overlap. Container settings do not affect the Pod's Volumes.

Here is the configuration file for a Pod that has one Container. Both the Pod and the Container have a securityContext field:

```shell
apiVersion: v1
kind: Pod
metadata:
  name: security-context-demo-2
spec:
  securityContext:
    runAsUser: 1000
  containers:
  - name: sec-ctx-demo-2
    image: gcr.io/google-samples/node-hello:1.0
    securityContext:
      runAsUser: 2000
      allowPrivilegeEscalation: false
```
Create the Pod:

```shell
kubectl apply -f https://k8s.io/examples/pods/security/security-context-2.yaml
```
Verify that the Pod's Container is running:

```shell
kubectl get pod security-context-demo-2
```
Get a shell into the running Container:

```shell
kubectl exec -it security-context-demo-2 -- sh
```
In your shell, list the running processes:

```shell
ps aux
```
The output shows that the processes are running as user 2000. This is the value of runAsUser specified for the Container. It overrides the value 1000 that is specified for the Pod.

```shell
USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
2000         1  0.0  0.0   4336   764 ?        Ss   20:36   0:00 /bin/sh -c node server.js
2000         8  0.1  0.5 772124 22604 ?        Sl   20:36   0:00 node server.js
...
```
Exit your shell:

```shell
exit
```

# Step 5. Implementing a Network Policy

## LIMIT traffic to an application

You can create Networking Policies allowing traffic from only
certain Pods.

**Use Case:**
- Restrict traffic to a service only to other microservices that need
  to use it.
- Restrict connections to a database only to the application using it.

![Diagram of LIMIT traffic to an application policy](images/2.gif)

### Example

Suppose your application is a REST API server, marked with labels `app=bookstore` and `role=api`:

    kubectl run --generator=run-pod/v1 apiserver --image=nginx --labels app=bookstore,role=api --expose --port 80

Save the following NetworkPolicy to `api-allow.yaml` to restrict the access
only to other pods (e.g. other microservices) running with label `app=bookstore`:

```yaml
kind: NetworkPolicy
apiVersion: networking.k8s.io/v1
metadata:
  name: api-allow
spec:
  podSelector:
    matchLabels:
      app: bookstore
      role: api
  ingress:
  - from:
      - podSelector:
          matchLabels:
            app: bookstore
```

```sh
$ kubectl apply -f api-allow.yaml
networkpolicy "api-allow" created
```

### Try it out

Test the Network Policy is **blocking** the traffic, by running a Pod without the `app=bookstore` label:

    $ kubectl run --generator=run-pod/v1 test-$RANDOM --rm -i -t --image=alpine -- sh
    / # wget -qO- --timeout=2 http://apiserver
    wget: download timed out

Traffic is blocked!

Test the Network Policy is **allowing** the traffic, by running a Pod with the `app=bookstore` label:

```shell
$ kubectl run --generator=run-pod/v1 test-$RANDOM --rm -i -t --image=alpine --labels app=bookstore,role=frontend -- sh
/ # wget -qO- --timeout=2 http://apiserver
<!DOCTYPE html>
<html><head>
```
Traffic is allowed.

### Cleanup

```shell
kubectl delete pod apiserver
kubectl delete service apiserver
kubectl delete networkpolicy api-allow
```