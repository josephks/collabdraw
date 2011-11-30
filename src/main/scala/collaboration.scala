package collabdraw

import unfiltered.request._
import unfiltered.netty.websockets._
import scala.collection.immutable.HashMap
import scala.actors.Actor

/** The main activity hub for drawing. All message routing for coordinating
 *  draw events passes through here. New users should be given the current
 *  state of the drawing, incoming messages will represent complete elements
 *  (for now, lines as path elements).
 */
class CollaborationPlan(drawings: DrawingStore, drawingActor: Actor)
  extends Plan
  with CloseOnException {
  
  @volatile
  var sockets =
    HashMap.empty[String, Set[WebSocket]].withDefaultValue(Set.empty[WebSocket])
  
  def intent = {
    case GET(Path(Seg("drawing" :: id :: Nil))) => {
      case Open(s) =>
        sockets += id -> (sockets(id) + s)
        (drawingActor !! FetchDrawing(id)) foreach( x => x match {
	  //I'm not sure in what thread this callback is executed in,
	  //so to be safe do the s.send in another thread
          case rawSvg: String => scala.actors.Actor.actor { s.send(rawSvg) }
	  case y => System.err.println("ERROR expected string, got: "+y)
        })
      case Message(s, Text(msg)) =>
        /* Message downstream clients with the new stroke and update
          the shared drawing. */
      case Close(s) =>
        /* Remove the associated socket. */
      case Error(s, e) =>
        /* Remove the associated socket. */
        e.printStackTrace
    }
  }
  
  def pass = Plan.DefaultPassHandler
}
