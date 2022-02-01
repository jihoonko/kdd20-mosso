# MoSSo: Lossless Graph Summarization in Fully Dynamic Graph Streams
Source code for MoSSo, described in the paper [Incremental Lossless Graph Summarization](https://arxiv.org/abs/2006.09935), Jihoon Ko*, Yunbum Kook* and Kijung Shin, KDD 2020.

**MoSSo** (**Mo**ve if **S**aved, **S**tay **o**thewise) is an algorithm for lossless summarization of fully dynamic graphs. **MoSSo** has the following advantages:
* *Fast and ’any time’*: processing each change in near-constant time, up to 7-orders of magnitude faster than running state-of-the-art batch methods.
* *Scalable*: summarizing graphs with hundreds of millions of edges, requiring sublinear memory during the process.
* *Effective*: achieving comparable compression ratios even to state-of-the-art batch methods.

## Building and Running **MoSSo**
Please see [User Guide](user_guide.pdf)

## Datasets and Contributors
The datasets used in the paper and authors information are listed [here](http://dmlab.kaist.ac.kr/mosso/)

## Terms and Conditions
If you use this code as part of any published research, please consider acknowledging our KDD 2020 paper.

```
@inproceedings{ko2020incremental,
  title={Incremental Lossless Graph Summarization},
  author={Ko, Jihoon and Kook, Yunbum and Shin, Kijung},
  booktitle={ACM SIGKDD International Conference on Knowledge Discovery and Data Mining},
  year={2020},
}
```
