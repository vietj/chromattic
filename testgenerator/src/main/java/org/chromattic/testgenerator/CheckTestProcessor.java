/*
* Copyright (C) 2003-2009 eXo Platform SAS.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.chromattic.testgenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
@SupportedSourceVersion(SourceVersion.RELEASE_5)
@SupportedAnnotationTypes(
      {
            "org.chromattic.testgenerator.GroovyTestGeneration",
            "org.chromattic.testgenerator.TestId"
      }
)
public class CheckTestProcessor extends AbstractProcessor
{
   private final Set<TestRef> generatedTests = new HashSet<TestRef>();
   private TestId testId;
   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment)
   {
      Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(TestId.class);

      if (elements.size() != 0) {
         testId = elements.iterator().next().getAnnotation(TestId.class);
      }

      for (Element element : roundEnvironment.getElementsAnnotatedWith(GroovyTestGeneration.class))
      {
         TypeElement typeElt = (TypeElement) element;
         TestRef ref = new TestRef(typeElt.getQualifiedName().toString());
         List<String> chromatticClassNames = SourceUtil.getChromatticClassName(typeElt);
         for (String chromatticQualifiedClassName : chromatticClassNames)
         {
            String chromatticName =
                  GroovyOutputFormat.CHROMATTIC.getPackageName(chromatticQualifiedClassName) + "." +
                  GroovyOutputFormat.CHROMATTIC.getClassName(chromatticQualifiedClassName).toString();
            ref.getChromatticObject().add(chromatticName);
         }
         generatedTests.add(ref);
      }


      if (roundEnvironment.processingOver())
      {
         try
         {
            String name = "testsRef-" + testId.value() + ".xml";
            FileObject xmlFile = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "load", name);
            Writer xmlWriter = xmlFile.openWriter();
            TestSerializer xmlSerializer = new TestSerializer();
            xmlSerializer.writeTo(xmlWriter, generatedTests);
            xmlWriter.close();
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      return false;
   }
}
