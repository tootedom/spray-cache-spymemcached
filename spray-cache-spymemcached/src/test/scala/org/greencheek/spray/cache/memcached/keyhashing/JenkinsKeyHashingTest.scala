package org.greencheek.spray.cache.memcached.keyhashing

import org.junit.Assert._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.scalatest.Spec
import org.specs2.mutable.Specification

/**
 * Created by dominictootell on 28/05/2014.
 */
@RunWith(classOf[JUnitRunner])
class JenkinsKeyHashingTest extends Specification {

  private val properties: Map[String, String] = Map[String, String](
  "sausage"->"2834523395",
  "blubber"->"1103975961",
  "pencil"->"3318404908",
  "cloud"->"670342857",
  "moon"->"2385442906",
  "water"->"3403519606",
  "computer"->"2375101981",
  "school"->"1513618861",
  "network"->"2981967937",
  "hammer"->"1218821080")

  "A Jenkins Hash" should  {
    import scala.collection.JavaConversions._
    for (entry <- properties.entrySet) {
      val result: String = JenkinsKeyHashing.hashKey(entry.getKey)
      if (result != entry.getValue) {
        System.out.println("Key: " + entry.getKey)
        System.out.println("Expected Hash Value: " + entry.getValue)
        System.out.println("Actual Hash Value: " + result)
      }
      assertEquals(result, entry.getValue)
    }
  }
}
