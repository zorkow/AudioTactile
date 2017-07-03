var xmldom = require('xmldom');
var exec = require('child_process').exec;

const dp = new xmldom.DOMParser();
let newDoc = (new xmldom.DOMImplementation()).createDocument('', '', 0);

let baseDir = '/home/sorge/git/Progressive/physics-diagrams/mockups/';

var withElements = true;

var circuits = {};
var elements = {};
var splits = [];
var xmlElements = {};
var replacements = {};
var duplicates = [];
var annotations = null;

convertFile = function(input, out) {
  circuits = {};
  elements = {};
  splits = [];
  xmlElements = {};
  replacements = {};
  duplicates = [];
  var svg = loadFile(input);
  var xml = svg.getElementsByTagName('title');
  var output = rewriteTitles(xml);
  saveFile(out, formatXml(output.toString()));
  exec('sed -i s/xmlns=/xmlns:sre=/g ' + out);
};

loadFile = function(name) {
  var input = fs.readFileSync(name, {encoding: 'utf8'});
  var xml = dp.parseFromString(input);
  return xml;
};

saveFile = function(out, output) {
  console.log('Writing ' + out);
  fs.writeFileSync(out, output);
};

addCircuits = function(circuit, component) {
  var existing = circuits[circuit];
  if (existing) {
    existing.push(component);
    return;
  }
  circuits[circuit] = [component];
};


addElements = function(element, component) {
  var existing = elements[element];
  if (existing) {
    existing.push(component);
    return;
  }
  elements[element] = [component];
};


processTitle = function(title, id) {
  var elements = title.match(/E[0-9]+/g) || [];
  for (var i = 0, element; element = elements[i]; i++) {
    addElements(element, id);
    title = title.replace(element, '');
  }

  var circuits = title.match(/C[0-9]+/g) || [];
  for (var i = 0, circuit; circuit = circuits[i]; i++) {
    addCircuits(circuit, id);
    title = title.replace(circuit, '');
  }
  if (title.match(/SPLIT/g)) {
    splits.push(id);
    title = title.replace('SPLIT', '');
  }
  return title.trimRight();
};

class XmlElement {

  constructor(id,type, speech, speech2, children, component) {
    this.id = id;
    this.type = type;
    this.speech = speech;
    this.speech2 = speech2;
    this.children = children;
    this.component = component;
    registerXmlElement(this);
  }

  makeNode(position, parent) {
    console.log("Making new node: " + this.id);
    if (duplicates.indexOf(this.id + '-' + parent) !== -1) {
      return;
    }
    duplicates.push(this.id + '-' + parent);
    var annotation = newDoc.createElement('sre:annotation');
    annotations.appendChild(annotation);
    createNode(annotation, this.type, this.id);
    annotation.setAttribute('speech', this.speech);
    annotation.setAttribute('speech2', this.speech2);
    createNode(annotation, 'position', position);
    if (parent) {
      var parents = newDoc.createElement('sre:parents');
      createNode(parents, 'grouped', parent);
      annotation.appendChild(parents);
    }
    if (this.children.length) {
      var children = newDoc.createElement('sre:children');
      annotation.appendChild(children);
      for (var i = 0; i < this.children.length; i++) {
        createNode(children, xmlElements[this.children[i]].type,
                   this.children[i]);
      }
    }
    if (this.component.length) {
      var component = newDoc.createElement('sre:component');
      annotation.appendChild(component);
      for (i = 0; i < this.component.length; i++) {
        createNode(component, 'active', this.component[i]);
      }
    }
  }

};


registerXmlElement = function(element) {
  xmlElements[element.id] = element;
};


rewriteTitles = function(xml) {
  var circuit = newDoc.createElementNS(
    'http://www.chemaccess.org/sre-schema', 'circuit');
  annotations = newDoc.createElement('sre:annotations');
  circuit.appendChild(annotations);
  var activeNodes = [];
  for (var i = 0; i < xml.length - 1; i++) {
    var id = xml[i].parentNode.getAttribute('id');
    var title = processTitle(xml[i].textContent, id);
    var sibling = xml[i].nextSibling;
    var descr = sibling ? sibling.textContent : title;
    var element = new XmlElement(id, 'active', title, descr, [], []);
    // element.makeNode(i + 1, 'top');
    activeNodes.push(id);
  }
  if (withElements) generateElements();
  i = 1;
  for (id in circuits) {
    var components = circuits[id];
    var children = components.map(function(child) {
      return replacements[child] || child;
    });
    var node = new XmlElement(id, 'grouped', id, id + '-descr',
                              children, components);
    for (var j = 0, child; child = children[j]; j++) {
      xmlElements[child].makeNode(j + 1, id);
    }
    node.makeNode(i++, 'top');
  }
  var top = xml[xml.length - 1];
  title = top.textContent;
  sibling = top.nextSibling;
  descr = sibling ? sibling.textContent : title;
  element = new XmlElement(
      'top', 'grouped', title, descr, Object.keys(circuits), activeNodes);
  element.makeNode(1, '');
  return circuit;
};

generateElements = function() {
  for (var id in elements) {
    var children = elements[id];
    var node = new XmlElement(id, 'grouped', id, id + '-descr',
                              children, children);
    for (var j = 0, child; child = children[j]; j++) {
      xmlElements[child].makeNode(j + 1, id);
      replacements[child] = id;
    }
  }
}


createNode = function(parent, tag, content) {
  var node = newDoc.createElement('sre:' + tag);
  var text = newDoc.createTextNode(content);
  node.appendChild(text);
  parent.appendChild(node);
  return node;
};



/**
 * Pretty prints an XML representation.
 * @param {string} xml The serialised XML string.
 * @return {string} The formatted string.
 */
formatXml = function(xml) {
  var reg = /(>)(<)(\/*)/g;
  xml = xml.replace(reg, '$1\r\n$2$3');
  reg = /(>)(.+)(<c)/g;
  xml = xml.replace(reg, '$1\r\n$2\r\n$3');
  var formatted = '';
  var padding = '';
  xml.split('\r\n')
      .forEach(function(node) {
        if (node.match(/.+<\/\w[^>]*>$/)) {
          // Node with content.
          formatted += padding + node + '\r\n';
        } else if (node.match(/^<\/\w/)) {
          if (padding) {
            // Closing tag
            padding = padding.slice(2);
            formatted += padding + node + '\r\n';
          }
        } else if (node.match(/^<\w[^>]*[^\/]>.*$/)) {
          // Opening tag
          formatted += padding + node + '\r\n';
          padding += '  ';
        } else {
          // Empty tag
          formatted += padding + node + '\r\n';
        }
      });
  return formatted;
};
