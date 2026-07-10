import cats.effect.{ExitCode, IO, IOApp}
import cortex.domain.learning.{ContentId, ContentProgress, LearningEvent, Note}

import scala.concurrent.duration.{Duration, DurationInt}

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
//    val events: List[LearningEvent] = List(
//      LearningEvent.ContentQueued(ContentId("id-1"), Book),
//      LearningEvent.ContentStarted(ContentId("id-1")),
//      LearningEvent.ContentAbandoned(ContentId("id-1")),
//      LearningEvent.ProgressUpdated(ContentId("id-1"), ContentProgress.BookAt(25)),
//      LearningEvent.NoteAdded(ContentId("id-1"), Note("my little note")),
//      LearningEvent.NoteAdded(ContentId("id-1"), Note("my little note2")),
//      LearningEvent.ContentCompleted(ContentId("id-1"))
//    )
//
//    val events2 = List(
//      ContentQueued(ContentId("id-1"), ContentKind.Book),
//      ContentQueued(ContentId("id-2"), ContentKind.Video),
//      ContentStarted(ContentId("id-1")),
//      ProgressUpdated(ContentId("id-2"), ContentProgress.VideoAt(120.seconds)),
//      ContentCompleted(ContentId("id-1"))
//    )
//
//    println(foldAll(events2))
//
//    val todoState       = ContentState(ContentId("id-1"), ContentKind.Book, ContentStatus.Todo, None, Vector.empty)
//    val inProgressState =
//      ContentState(ContentId("id-1"), ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
//    val completedState  = ContentState(ContentId("id-1"), ContentKind.Book, ContentStatus.Completed, None, Vector.empty)
//    val abandonedState  = ContentState(ContentId("id-1"), ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
//
////    println(decide(Some(todoState), Command.Start))
////    println(decide(Some(inProgressState), Command.Start))
////    println(decide(Some(completedState), Command.Complete))
////    println(decide(Some(todoState), Command.Complete))
////    println(decide(Some(abandonedState), Command.Resume))
////    println(decide(None, Command.Start))
//
//    val res = events.foldLeft(Option.empty[ContentState])(evolve)

    IO.println("Hello world!")
      >> IO.pure(ExitCode.Success)
