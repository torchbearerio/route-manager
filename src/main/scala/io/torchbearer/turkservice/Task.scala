package io.torchbearer.turkservice

import com.amazonaws.mturk.requester.QualificationRequirement

sealed class Task(val name: String,
                           val title: String,
                           val description: String,
                           val reward: Double,
                           val keywords: String,
                           val autoApprovalDelay: Long = 172800,
                           val assingmentDuration: Long = 60,
                           val qualificationRequirements: Array[QualificationRequirement] = Array()) {
  val hitTypeId: String = registerHitType

  private def registerHitType: String = {
    val turkClient = TurkClientFactory.getClient
    val t = this
    turkClient.registerHITType(t.autoApprovalDelay, t.assingmentDuration, t.reward, t.title, t.keywords, t.description, t.qualificationRequirements)
  }
}

case object SALIENCY_DETECTION extends Task("Saliency Detection",
  "Image Landmark Selection",
  "Draw a box around the most prominent feature in an image",
  0.05,
  "image annotation,image tagging,directions,navigation")

case object OBJECT_DESCRIPTION extends Task("Object Description",
  "Image Description",
  "Provide a few words to describe the pictured landmark",
  0.05,
  "image annotation,image tagging,directions,navigation")