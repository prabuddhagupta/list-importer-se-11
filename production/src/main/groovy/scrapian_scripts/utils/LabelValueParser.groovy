package scrapian_scripts.utils

import com.rdc.importer.scrapian.model.StringSource

class LabelValueParser {
  def htmlToText(input, params, regEx = null) { //regEx is STRING and good one I find recently is "&[a-z#0-9]+?;"
    def html = new StringSource(input.toString())
    def text;

    if (params.replaceElements) {
      def count = 0;
      params.replaceElements.each {element, value ->
        html.replace(element, count++ + "#REPLACE#");
      }
      html.stripXmlTags().stripFormatting().convertNbsp().stripEntities(regEx);
      count = 0;
      params.replaceElements.each {element, value ->
        html.replace(count++ + "#REPLACE#", value);
      }
      text = html.toString()
    } else {
      text = html.stripXmlTags().stripFormatting().convertNbsp().stripEntities(regEx).toString();
    }

    if (params.breakOnLabels != false) {
      def labels = flattenLabels(params, false);
      labels.each {label ->
        def startIndex = 0;
        def matchIndex = text.indexOf(label, startIndex);
        while (matchIndex != -1) {
          // now we must check that it isn't part of another label - For example: Date of Conviction: and Conviction:
          def foundLargerMatch = false;
          labels.each {label2 ->
            if (label2.contains(label) && !label2.equals(label)) {
              def index2 = text.indexOf(label2, startIndex)
              def endIndex = index2 + label2.length() - label.length();
              if (index2 < matchIndex && (endIndex) == matchIndex) {
                foundLargerMatch = true;
              }
            }
          }
          if (!foundLargerMatch) {
            text = text.substring(0, matchIndex) + "\n\n" + text.substring(matchIndex);
          }
          startIndex = startIndex + matchIndex + label.length();  //TODO? remove startIndex from assignment
          matchIndex = text.indexOf(label, startIndex);
        }
      }
    }

    return text;
  }

  def flattenLabels(params, complex) {
    def labels = [];
    params.labels.each {label ->
      if (params.dividers) {
        params.dividers.each {divider ->
          if (complex) {
            labels.add([label: label, value: label + divider]);
          } else {
            labels.add(label + divider)
          }
        }
      } else {
        if (complex) {
          labels.add([label: label, value: label]);
        } else {
          labels.add(label);
        }
      }
    }
    return labels;
  }

  def parse(text, params) {
    def currentLabelMatch = null;
    def dataMap = [:]
    def multiLineSeparator = ' ';
    if (params.multiLineSeparator) {
      multiLineSeparator = params.multiLineSeparator;
    }
    def complexLabels = flattenLabels(params, true)
    text.toString().split("\n").each {line ->
      def newLabelMatch = getLabelMatch(line, complexLabels);
      if (newLabelMatch) {
        def value = stripLabel(newLabelMatch, line)
        value = cleanValue(value, params)        
        if(!newLabelMatch.label.equals("Name") || (!params.firstInstanceOfNameFoundOnly || dataMap["Name"]==null)) {
            dataMap[newLabelMatch.label] = value;
        }
        currentLabelMatch = newLabelMatch;
      }
      if (!newLabelMatch && params.multiLineValues) {
        if (lineContainsDividers(line, params)) {
          currentLabelMatch = null; // label that we didn't specifically ask for...
        } else {
          // append subsequent lines to current label match until blank line is hit
          if (currentLabelMatch && line.trim() != "") {
            line = cleanValue(line, params)
            dataMap[currentLabelMatch.label] = dataMap[currentLabelMatch.label] + multiLineSeparator + line;
          } else if (line.trim() == "") {
            currentLabelMatch = null;
          }
        }
      }
    }
    return dataMap;
  }

  private def getLabelMatch(line, complexLabels) {
    def labelMatch = null;
    def labelMatchLength = -1;
    complexLabels.each {label ->
      if (line.toUpperCase().startsWith(label.value.toUpperCase())) {
        if (labelMatch) {
          if (label.value.length() > labelMatchLength) { // find longest label matches - For example "Charge:" vs "Charges:" would both match.
            labelMatch = label;
            labelMatchLength = label.value.length();
          }
        } else {
          labelMatch = label;
          labelMatchLength = label.value.length();
        }
      }
    }
    return labelMatch;
  }

  private def cleanValue(value, params) {
    if (params.escape) {
      value = value.replaceAll("&", "&amp;");
      value = value.replaceAll("<", "&lt;");
      value = value.replaceAll(">", "&gt;");
    }
    return new StringSource(value).stripHtmlTags().stripFormatting().toString().trim();
  }

  private def stripLabel(complexLabel, line) {
    def value = line.substring(complexLabel.value.length()).trim();
    // strip spacers ...
    while (value.startsWith(".")) {
      value = value.substring(1).trim();
    }
    return new StringSource(value).stripFormatting().toString().trim();
  }

  private def lineContainsDividers(value, definition) {
    if (definition.dividers) {
      def matches = definition.dividers.find {divider ->
        value.lastIndexOf(divider) != -1
      }
      return matches != null
    }
    return false;
  }
}