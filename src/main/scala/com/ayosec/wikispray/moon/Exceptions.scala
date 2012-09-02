package com.ayosec.wikispray.moon

class MoonError extends Exception
class DocumentNotFound(val collection: MoonCollection, val query: com.mongodb.DBObject) extends MoonError
