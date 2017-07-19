package sbtbuildinfo

case class ScalaCaseClassRenderer(options: Seq[BuildInfoOption], pkg: String, obj: String) extends ScalaRenderer {
  override def fileType = BuildInfoType.Source
  override def extension = "scala"

  val traitNames = options.collect{case BuildInfoOption.Traits(ts @ _*) => ts}.flatten
  val objTraits = if (traitNames.isEmpty) "" else " extends " ++ traitNames.mkString(" with ")

  // It is safe to add `import scala.Predef` even though we need to keep `-Ywarn-unused-import` in mind
  // because we always generate code that has a reference to `String`. If the "base" generated code were to be
  // changed and no longer contain a reference to `String`, we would need to remove `import scala.Predef` and
  // fully qualify every reference. Note it is NOT safe to use `import scala._` because of the possibility of
  // the project using `-Ywarn-unused-import` because we do not always generated references that are part of
  // `scala` such as `scala.Option`.
  def header = List(
    s"package $pkg",
    "",
    "import scala.Predef._",
    "",
    s"/** This file was generated by sbt-buildinfo. */"
  )

  override def renderKeys(buildInfoResults: Seq[BuildInfoResult]) =
    header ++
    caseClassDefinitionBegin ++
      buildInfoResults.flatMap(caseClassParameter).mkString(",\n").split("\n") ++
      caseClassDefinitionEnd ++
      toMapMethod(buildInfoResults) ++
      caseClassEnd ++
      List("") ++
      caseObjectLine(buildInfoResults)

  private def caseClassDefinitionBegin = List(
    s"case class $obj$objTraits("
  )

  private def caseClassParameter(r: BuildInfoResult): Seq[String] = {
    val typeDecl = getType(r.typeExpr) getOrElse "Any"

    List(
      s"  ${r.identifier}: $typeDecl"
    )
  }

  private def toMapMethod(results: Seq[BuildInfoResult]) =
    if (options.contains(BuildInfoOption.ToMap))
      results
        .map(result => "    \"%s\" -> %s".format(result.identifier, result.identifier))
        .mkString("  def toMap: Map[String, Any] = Map[String, Any](\n", ",\n", ")")
        .split("\n")
        .toList ::: List("")
    else Nil

  private def caseClassDefinitionEnd = List(") {", "")
  private def caseClassEnd = List("}")

  private def caseObjectLine(buildInfoResults: Seq[BuildInfoResult]) = List(
    s"case object $obj {",
    s"  def apply(): $obj = new $obj(${buildInfoResults.map(_.value).map(quote).mkString(",")})",
    s"  val get = apply()",
    s"  val value = apply()",
    s"}"
  )

  def toMapLine(results: Seq[BuildInfoResult]): Seq[String] =
    if (options.contains(BuildInfoOption.ToMap) || options.contains(BuildInfoOption.ToJson))
      results
        .map(result => "    \"%s\" -> %s".format(result.identifier, result.identifier))
        .mkString("  val toMap: Map[String, Any] = Map[String, Any](\n", ",\n", ")")
        .split("\n")
        .toList ::: List("")
    else Nil
}