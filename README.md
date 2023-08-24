# kube-operator
An example project of an implementation of a Kubernetes operator written using the Java Operator Framework SDK

## Test
In order to get tests to pass, must first apply the generated CRD to the target cluster being tested on. The generated
yaml file should be in `target/META-INF/fabric8` post compilation