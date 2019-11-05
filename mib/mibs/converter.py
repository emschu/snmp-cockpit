#!/usr/bin/python3

import glob
import json
import os
# the following is a proof of concept example to generate
# an oid catalog + tree dynamically out of several files

blackList = {"index.json", "oid_tree.json", "oid_catalog.json"}
oidElements = {}

fileList = ("RFC1213-MIB.json", "*.json")

for mibName in fileList:
    path = os.path.dirname(os.path.realpath(__file__)) + "/" + mibName
    for filename in glob.glob(path):
        isBlackListed = False
        for blItem in blackList:
            if blItem in filename:
                isBlackListed = True

        if isBlackListed:
            print("skip blacklisted " + filename)
            continue
        print("importing file " + filename)
        with open(filename, 'r') as f:
            data = json.load(f)

        for key in data:
            value = data[key]
            isSelected = False
            if value != False:
                for elementProperty in value:
                    if elementProperty == "oid":
                        if value[elementProperty] in oidElements:
                            print("skip duplicate " + value[elementProperty])
                            continue
                        oidElements[value[elementProperty]] = value
                        isSelected = True
                        print("added " + value[elementProperty])
                        break
                if not isSelected:
                    print("not selected " + key)

# if you want to remove read-write entries, uncomment the following lines
# readWriteOids = []
# for oid in oidElements:
#     queryContent = oidElements[oid]
#     if "maxaccess" in queryContent:
#         if queryContent["maxaccess"] == "read-write":
#             readWriteOids.append(oid)
#
# for oidToDelete in readWriteOids:
#     oidElements.pop(oidToDelete)
#     print("removing read-write oid " + oidToDelete)

# write oid_catalog.json
# dump oid catalog file
with open('oid_catalog.json', 'w') as fp:
    json.dump(oidElements, fp)

#oidElements.pop("0.0")


# OID Trie class
class Trie:
    def __init__(self):
        self.root = self.getNode()

    def getNode(self):
        return {"isLeaf": False, "children": {}}

    def insertWord(self, word, oid, name):
        current = self.root
        # split nodes by "."
        for ch in word.split('.'):
            if ch in current["children"]:
                node = current["children"][ch]
            else:
                node = self.getNode()
                current["children"][ch] = node
            current = node
        current["isLeaf"] = True
        current["oidValue"] = oid
        current["name"] = name

# remove zeroDotZero
oidElements.pop('0.0')

rtree = Trie()
for oid in oidElements:
    rtree.insertWord(oid, oid, oidElements[oid]['name'])

# write oid_tree.json
with open('oid_tree.json', 'w') as fp:
    json.dump(rtree.root, fp)
