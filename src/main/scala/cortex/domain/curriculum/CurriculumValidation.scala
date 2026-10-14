package cortex.domain.curriculum

import cats.data.ValidatedNec
import cats.syntax.all.*

private[curriculum] object CurriculumValidation:
  def nonEmpty[E](value: String, error: E): ValidatedNec[E, String] =
    val normalized = value.trim
    if normalized.nonEmpty then normalized.validNec
    else error.invalidNec

  def unique[A, E](
    values: Vector[A],
    error: Set[A] => E
  ): ValidatedNec[E, Unit] =
    val duplicates =
      values.groupBy(identity).collect { case (value, xs) if xs.size > 1 => value }.toSet

    if duplicates.isEmpty then ().validNec
    else error(duplicates).invalidNec

  def nonEmptyKeys[A, K, E](
    values: Vector[A],
    text: A => String,
    key: A => K,
    error: Set[K] => E
  ): ValidatedNec[E, Unit] =
    val empty = values.collect {
      case value if text(value).trim.isEmpty => key(value)
    }.toSet

    if empty.isEmpty then ().validNec
    else error(empty).invalidNec
