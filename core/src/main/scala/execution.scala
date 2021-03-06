package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler
}
import java.util.{concurrent => juc}

class Http extends Executor { self =>
  lazy val client = new AsyncHttpClient
  val timeout = Duration.Zero

  /** Convenience method for an Executor with the given timeout */
  def waiting(t: Duration) = new Executor {
    def client = self.client
    def timeout = t
  }
}

object Http extends Http

trait Executor {
  def client: AsyncHttpClient
  /** Timeout for promises made by this HTTP Executor */
  def timeout: Duration

  def apply(builder: RequestBuilder): Promise[Response] =
    apply(builder.build() -> new FunctionHandler(identity))

  def apply[T](pair: (Request, AsyncHandler[T])): Promise[T] =
    apply(pair._1, pair._2)

  def apply[T](request: Request, handler: AsyncHandler[T]): Promise[T] =
    new ListenableFuturePromise(
      client.executeRequest(request, handler),
      client.getConfig.executorService,
      timeout
    )

  def shutdown() {
    client.close()
  }
}
