package cortex.domain.catalog

final case class Resource private (
  id: ResourceId,
  title: String,
  kind: ResourceKind,
  author: Option[String],
  url: Option[String],
  size: Option[ResourceSize]
)

object Resource:
  def make(
    id: ResourceId,
    title: String,
    kind: ResourceKind,
    author: Option[String] = None,
    url: Option[String] = None,
    size: Option[ResourceSize] = None
  ): Either[ResourceError, Resource] =
    for
      normalizedTitle <- validateTitle(title)
      normalizedUrl   <- validateUrl(url)
      _               <- validateSize(kind, size)
    yield Resource(id, normalizedTitle, kind, author, normalizedUrl, size)

  private def validateTitle(title: String): Either[ResourceError, String] =
    val normalized = title.trim
    Either.cond(normalized.nonEmpty, normalized, ResourceError.EmptyTitle)

  private def validateUrl(url: Option[String]): Either[ResourceError, Option[String]] =
    url match
      case None => Right(None)
      case Some(value) =>
        val normalized = value.trim
        Either.cond(normalized.nonEmpty, Some(normalized), ResourceError.EmptyUrl)

  private def validateSize(
    kind: ResourceKind,
    size: Option[ResourceSize]
  ): Either[ResourceError, Unit] =
    size match
      case None => Right(())
      case Some(ResourceSize.Pages(_)) if kind == ResourceKind.Article || kind == ResourceKind.Book =>
        Right(())
      case Some(ResourceSize.Duration(_)) if kind == ResourceKind.Video || kind == ResourceKind.Course =>
        Right(())
      case Some(value) => Left(ResourceError.InvalidSize(kind, value))

enum ResourceError:
  case EmptyTitle
  case EmptyUrl
  case InvalidSize(kind: ResourceKind, size: ResourceSize)
