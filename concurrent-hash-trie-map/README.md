# Concurrent Tries (CTries) in Java

Welcome to the Concurrent Tries (CTries) project! This project is a direct implementation of the data structure described in the paper "[Concurrent Tries with Efficient Non-Blocking Snapshots](http://aleksandar-prokopec.com/resources/docs/ctries-snapshot.pdf)" by Aleksandar Prokopec. This implementation is inspired by the Scala implementation of CTries, which can be found on Aleksandar Prokopec's [GitHub repository](https://github.com/axel22/Ctries).

## Overview

Concurrent Tries (CTries) are a highly concurrent and lock-free data structure designed for efficient in-memory storage and retrieval of key-value pairs. They support scalable concurrent operations and efficient non-blocking snapshots, making them ideal for applications requiring high throughput and low latency.

This project provides a Java implementation of CTries, preserving the original design principles and optimizations described in the paper. It aims to offer a reliable and performant alternative to traditional concurrent data structures like `ConcurrentHashMap`.

## Features

- **Lock-Free Operations**: CTries offer lock-free inserts, removes, and lookups, enabling high levels of concurrency.
- **Efficient Snapshots**: The snapshot mechanism allows for efficient, non-blocking snapshots of the trie, facilitating consistent views of the data at any point in time.
- **Scalability**: Designed to scale with the number of threads, CTries perform well under high contention.
- **Memory Efficiency**: Compressed nodes help reduce memory overhead, especially in sparse tries.

## Structure

The project is organized into several core components:

- **ConcurrentTrie**: The main class representing the concurrent trie.
- **IndirectionNode**: Nodes used to manage references to main nodes in a lock-free manner.
- **CompressedNode**: Nodes that store key-value pairs and sub-nodes in a compressed format using bitmaps.
- **SingletonNode**: Nodes that store individual key-value pairs.
- **ListNode**: Nodes used for handling hash collisions with linked lists.
- **TombNode**: Nodes representing deleted entries for logical removal.

## Installation

Clone the repository and build the project using your preferred build tool (e.g., Maven or Gradle).

```sh
git clone https://github.com/nitin966/datastructures.git
cd datastructures/concurrent-hash-trie-map
```

## Usage

Here's a simple example of how to use the ConcurrentTrie:

```java
import com.ctrie.ConcurrentTrie;

public class CTrieExample {
    public static void main(String[] args) {
        ConcurrentTrie<String, Integer> trie = new ConcurrentTrie<>();
        
        // Insert key-value pairs
        trie.put("apple", 1);
        trie.put("banana", 2);
        
        // Lookup values
        System.out.println("Value for 'apple': " + trie.get("apple"));
        
        // Remove a key
        trie.remove("banana");
        
        // Check existence
        System.out.println("Contains 'banana': " + trie.containsKey("banana"));
    }
}
```

## Documentation

For detailed API documentation, please refer to the Javadoc comments in the source code. Additionally, the [original paper](http://aleksandar-prokopec.com/resources/docs/ctries-snapshot.pdf) provides in-depth explanations of the data structure and its properties.

## Contributing

Contributions are welcome! Please fork the repository and submit pull requests for any improvements or bug fixes. Ensure your code adheres to the project's coding standards and includes appropriate tests.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgements

This project is heavily inspired by the work of Aleksandar Prokopec and his [Scala implementation of CTries](https://github.com/axel22/Ctries). We thank him for his pioneering work in the field of concurrent data structures.

---

Feel free to open issues for any questions or suggestions. Happy coding!
