/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.kv.experimental

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.withContext
import net.consensys.cava.bytes.Bytes
import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * A key-value store backed by LevelDB.
 *
 * @param dbPath The path to the levelDB database.
 * @param options Options for the levelDB database.
 * @param dispatcher The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a LevelDB-backed key-value store.
 */
class LevelDBKeyValueStore
@JvmOverloads
@Throws(IOException::class)
constructor(
  dbPath: Path,
  options: Options = Options().createIfMissing(true).cacheSize((100 * 1048576).toLong()),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeyValueStore, net.consensys.cava.kv.LevelDBKeyValueStore {

  private val db: DB

  init {
    Files.createDirectories(dbPath)
    db = JniDBFactory.factory.open(dbPath.toFile(), options)
  }

  override suspend fun get(key: Bytes): Bytes? = withContext(dispatcher) {
    val rawValue = db[key.toArrayUnsafe()]
    if (rawValue == null) {
      null
    } else {
      Bytes.wrap(rawValue)
    }
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(dispatcher) {
    db.put(key.toArrayUnsafe(), value.toArrayUnsafe())
  }

  /**
   * Closes the underlying LevelDB instance.
   */
  override fun close() = db.close()
}