# Asynchronous Request Processing

Asynchronous request processing allows clients to submit a request,
and get the results of the request later, or never. The implementation
has three parts:

 * The front end that accepts asynchronous requests, and lets clients retrieve results later,
 * The job store,
 * The executor that executes the queued jobs.

The front and end the executor are define in lightblue core. The job
store is specified as an interface, and can be defined in a
persistence backend, or in a separate class. Lightblue core does not
dictate how asynchronous processing capabilities are deployed. The
executor and the front-end can be deployed as a single application, or
separate applications. This allows different deployment scenarios:

  * Regular CRUD nodes can be both front-ends to asynchronous requests
    and asynchronous executors
  * Regular CRUD nodes can accept asynchronous requests, but dedicated nodes can be asynchronous
    executors.
  * Asynchronous request front-ends and executors can be a separate cluster

## Operation Overview

Clients use AsynchRequest or AsynchBulkRequest instances to submit
requests. These classes contain a regular Request or BulkRequest
object along with asynchronous job scheduling options. The front end
method is Madiator.submitAsynchRequest(). This method simply creates a
new job in the job store, and returns the job identifier to the
client. The client then calls Mediator.getAsynchResponse() using this
job identifier to retrieve job status. The job status contains an
indicator telling the client whether the execution has been completed
or not. If the execution has not been completed, the client should ask
for the results later. Once the execution is completed, execution
results will be returned in the job status, and the job will be
removed from the job store.

## Configuration

The asynchronous request processing configuration is defined in crud
configuration (lightblue-crud.json). The configuration bean is
com.redhat.lightblue.extensions.asynch.AsynchronousExecutionConfiguration,
and parsed in CrudConfiguration class, and stored in Factory. The
expected configuration is as follows:

```
{
  "asynchronousExecutions": {
     "backend": <backendName>,
     "options": { <backend specific options> }
  }
  ...
}

or

{
  "asynchronousExecutions": {
     "schedulerClass": <className>,
     "options": { <backend specific options> }
  }
  ...
}
```

 * backend: This gives the name of the CRUD controller that implements
   AsynchronousExecutionSupport interface. The backend must be defined
   in the same file as a CRUD controller.
 * schedulerClass: This gives the class name of an AsynchronousExecutionSupport
   implementation.
 * options: A JSON object defined based on the asynchronous execution support implementation.

Only one of "backend" or "schedulerClass" can be defined. With
"backend", an existing CRUD controller implements the job store and
scheduling functionality. With "schedulerClass", a custom job store
implementation can be used.

## Executor

The executor is defined in the asynch project. It is a threaded
polling implementation. It operates as follows:

  * When started, a DriverThread must be created with the
    configuration and LightblueFactory instance. This thread is
    responsible for two things:
    * It creates a SchedulerThread, and
      makes sure one instance of SchedulerThread is always operational.
    * It checks the request processor threads, and interrupts those that have timed out.
  * The SchedulerThread retrieves the next job from the job store, and
    creates a RequestProcessorThread to process that request. It creates new
    RequestProcessorThreads up to a configured maximum as long as there are jobs available.
    If all jobs are processed, the SchedulerThread sleeps for a while, and then polls again.
  * Each RequestProcessorThread processes one request, and updates the job based on the results
    retrieved.
    

The DriverThread does not perform any I/O, and logs all exceptions it
receives without terminating. Thus, as long as the DriverThread is
alive, the executor processes jobs. Intermittent database errors will
create lots of logs, but those will only kill the scheduler and
request processor threads, which will be recreated by the DriverThread
later.

