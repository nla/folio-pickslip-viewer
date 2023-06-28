#!/bin/bash

DEST=$1
NLA_ENVIRON=$2

JARFILE=folio-pickslip-viewer.jar

mvn -U clean package 

mkdir -p $1/lib
cp target/*.jar $1/lib/$JARFILE

