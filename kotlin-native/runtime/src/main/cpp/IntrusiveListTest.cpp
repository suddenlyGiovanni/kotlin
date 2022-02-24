/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "IntrusiveList.hpp"

#include <forward_list>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"
#include "Utils.hpp"

using namespace kotlin;

namespace {

class Element {
public:
    explicit Element(int value) : value_(value) {}

    Element(const Element&) = default;
    Element(Element&&) = default;
    Element& operator=(const Element&) = default;
    Element& operator=(Element&&) = default;

    int& operator*() { return value_; }
    const int& operator*() const { return value_; }

    bool operator==(const Element& rhs) const { return value_ == rhs.value_; }

    bool operator!=(const Element& rhs) const { return !(*this == rhs); }

private:
    int value_;
};

class Node : private Pinned {
public:
    explicit Node(int value) : value_(value) {}

    int& operator*() { return value_; }
    const int& operator*() const { return value_; }

    Node* next() const { return next_; }
    void setNext(Node* next) { next_ = next; }

private:
    int value_;
    // Use non-null marker to make sure inserting into the list properly updates this value.
    Node* next_ = reinterpret_cast<Node*>(0x1);
};

class ForwardListTestNames {
public:
    template <typename T>
    static std::string GetName(int) {
        if constexpr (std::is_same_v<T, std::forward_list<Element>>) {
            return "forward_list";
        } else if constexpr (std::is_same_v<T, intrusive_forward_list<Node>>) {
            return "intrusive_forward_list";
        } else {
            return "unknown";
        }
    }
};

template <typename Value>
KStdList<Value> create(std::initializer_list<int> list) {
    KStdList<Value> result;
    for (auto x : list) {
        result.emplace_back(x);
    }
    return result;
}

template <typename List>
KStdVector<int> collect(const List& list) {
    KStdVector<int> result;
    for (const auto& x : list) {
        result.push_back(*x);
    }
    return result;
}

auto asIterator(std::forward_list<Element>::iterator it) {
    return it;
}

auto asBeforeBeginIterator(std::forward_list<Element>::iterator it) {
    return it;
}

auto asIterator(std::variant<intrusive_forward_list<Node>::iterator, intrusive_forward_list<Node>::const_before_begin_iterator> it) {
    return std::get<intrusive_forward_list<Node>::iterator>(it);
}

auto asBeforeBeginIterator(
        std::variant<intrusive_forward_list<Node>::iterator, intrusive_forward_list<Node>::const_before_begin_iterator> it) {
    return std::get<intrusive_forward_list<Node>::const_before_begin_iterator>(it);
}

} // namespace

TEST(IntrusiveForwardListTest, CTAD) {
    auto values = create<Node>({1, 2, 3, 4});
    intrusive_forward_list list(values.begin(), values.end());
    static_assert(std::is_same_v<decltype(list)::value_type, Node>);
}

// Testing that operations on `intrusive_forward_list` give the same results as those on `std::forward_list`.
template <typename T>
class ForwardListTest : public testing::Test {};

using ForwardListTestTypes = testing::Types<std::forward_list<Element>, intrusive_forward_list<Node>>;
TYPED_TEST_SUITE(ForwardListTest, ForwardListTestTypes, ForwardListTestNames);

TYPED_TEST(ForwardListTest, DefaultCtor) {
    using List = TypeParam;
    List list;
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, RangeCtor) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, RangeCtorEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({});
    List list(values.begin(), values.end());
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, MoveCtor) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList(std::move(list));
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, MoveAssignment) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    otherList = std::move(list);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, MutableIterator) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    for (auto& x : list) {
        *x += 5;
    }
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(6, 7, 8, 9));
}

TYPED_TEST(ForwardListTest, MutableIteratorEmpty) {
    using List = TypeParam;
    List list;
    for (auto& x : list) {
        *x += 5;
    }
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, MoveAssignmentIntoEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    otherList = std::move(list);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, MoveAssignmentFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    otherList = std::move(list);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, Swap) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    using std::swap;
    swap(list, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SwapFirstEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    using std::swap;
    swap(list, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SwapSecondEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    using std::swap;
    swap(list, otherList);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SwapBothEmpty) {
    using List = TypeParam;
    List list;
    List otherList;
    using std::swap;
    swap(list, otherList);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, Assign) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto newValues = create<Value>({5, 6, 7, 8});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
}

TYPED_TEST(ForwardListTest, AssignFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto newValues = create<Value>({});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, AssignIntoEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto newValues = create<Value>({5, 6, 7, 8});
    list.assign(newValues.begin(), newValues.end());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
}

TYPED_TEST(ForwardListTest, Front) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    EXPECT_THAT(*list.front(), 1);
    const List& constList = list;
    EXPECT_THAT(*constList.front(), 1);
}

TYPED_TEST(ForwardListTest, Clear) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.clear();
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, ClearEmpty) {
    using List = TypeParam;
    List list;
    list.clear();
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, PushFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    Value value(5);
    list.push_front(value);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, PushFrontEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    Value value(5);
    list.push_front(value);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
}

TYPED_TEST(ForwardListTest, PopFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.pop_front();
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(2, 3, 4));
}

TYPED_TEST(ForwardListTest, PopFrontIntoEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1});
    List list(values.begin(), values.end());
    list.pop_front();
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, Remove) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto& value = *std::next(list.begin(), 2);
    ASSERT_THAT(*value, 3);
    list.remove(value);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 4));
}

TYPED_TEST(ForwardListTest, RemoveMissing) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    Value value(5);
    list.remove(value);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, RemoveAll) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1});
    List list(values.begin(), values.end());
    auto& value = list.front();
    list.remove(value);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, RemoveEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    Value value(5);
    list.remove(value);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, RemoveIf) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return *x == 3; });
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 4));
}

TYPED_TEST(ForwardListTest, RemoveIfMissing) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return *x == 5; });
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, RemoveIfAll) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    list.remove_if([](const auto& x) { return true; });
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, RemoveIfEmpty) {
    using List = TypeParam;
    List list;
    list.remove_if([](const auto& x) { return *x == 3; });
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, RemoveIfAllEmpty) {
    using List = TypeParam;
    List list;
    list.remove_if([](const auto& x) { return true; });
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, InsertAfter) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    Value value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    Value value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, list.begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterFrontEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto it = list.before_begin();
    Value value(5);
    auto result = list.insert_after(it, value);
    EXPECT_THAT(result, list.begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
}

TYPED_TEST(ForwardListTest, InsertAfterRange) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto otherValues = create<Value>({5, 6, 7, 8});
    auto result = asIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, std::next(list.begin(), 6));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 6, 7, 8, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRange) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto otherValues = create<Value>({});
    auto result = asIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterRangeFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto otherValues = create<Value>({5, 6, 7, 8});
    auto result = asIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRangeFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto otherValues = create<Value>({});
    auto result = asBeforeBeginIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, list.before_begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, InsertAfterRangeFrontEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto it = list.before_begin();
    auto otherValues = create<Value>({5, 6, 7, 8});
    auto result = asIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, std::next(list.begin(), 3));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
}

TYPED_TEST(ForwardListTest, InsertAfterEmptyRangeFrontEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto it = list.before_begin();
    auto otherValues = create<Value>({});
    auto result = asBeforeBeginIterator(list.insert_after(it, otherValues.begin(), otherValues.end()));
    EXPECT_THAT(result, list.before_begin());
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, EraseAfter) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 1);
    ASSERT_THAT(**it, 2);
    auto result = list.erase_after(it);
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 4));
}

TYPED_TEST(ForwardListTest, EraseAfterNearEnd) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.end());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3));
}

TYPED_TEST(ForwardListTest, EraseAfterFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(2, 3, 4));
}

TYPED_TEST(ForwardListTest, EraseAfterToEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1});
    List list(values.begin(), values.end());
    auto it = list.before_begin();
    auto result = list.erase_after(it);
    EXPECT_THAT(result, list.end());
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, EraseAfterRange) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(list.begin(), it);
    EXPECT_THAT(result, std::next(list.begin(), 1));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 3, 4));
}

TYPED_TEST(ForwardListTest, EraseAfterRangeToEnd) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.begin(), list.end());
    EXPECT_THAT(result, list.end());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1));
}

TYPED_TEST(ForwardListTest, EraseAfterEmptyRange) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(std::next(list.begin(), 1), std::next(list.begin(), 2));
    EXPECT_THAT(result, std::next(list.begin(), 2));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, EraseAfterRangeFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto result = list.erase_after(list.before_begin(), it);
    EXPECT_THAT(result, list.begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(3, 4));
}

TYPED_TEST(ForwardListTest, EraseAfterRangeFrontToEnd) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.before_begin(), list.end());
    EXPECT_THAT(result, list.end());
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, EraseAfterEmptyRangeFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto result = list.erase_after(list.before_begin(), list.begin());
    EXPECT_THAT(result, list.begin());
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfter) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 6, 7, 8, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMove) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 6, 7, 8, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8, 1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterFrontFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    auto it = list.before_begin();
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFrontFromEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    List otherList;
    auto it = list.before_begin();
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmpty) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 6, 7, 8));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmptyFromEmpty) {
    using List = TypeParam;
    List list;
    List otherList;
    auto it = list.before_begin();
    list.splice_after(it, otherList);
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmptyFromEmpty) {
    using List = TypeParam;
    List list;
    List otherList;
    auto it = list.before_begin();
    list.splice_after(it, std::move(otherList));
    EXPECT_THAT(list.empty(), true);
    EXPECT_THAT(collect(list), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterSingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 7, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveSingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 7, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFrontSingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(7, 1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFrontSingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(7, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterSingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 8, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 7));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveSingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 8, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFrontSingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(8, 1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 7));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFrontSingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(8, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterSingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(6, 7, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveSingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFrontSingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(6, 7, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFrontSingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterSingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveSingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = std::next(list.begin(), 2);
    ASSERT_THAT(**it, 3);
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(1, 2, 3, 5, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterFrontSingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveFrontSingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    auto values = create<Value>({1, 2, 3, 4});
    List list(values.begin(), values.end());
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5, 1, 2, 3, 4));
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmptySingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(7));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmptySingle) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 1);
    ASSERT_THAT(**jt, 6);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(7));
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmptySingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(8));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(5, 6, 7));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmptySingleLast) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = std::next(otherList.begin(), 2);
    ASSERT_THAT(**jt, 7);
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(8));
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmptySingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
    EXPECT_THAT(otherList.empty(), false);
    EXPECT_THAT(collect(otherList), testing::ElementsAre(6, 7, 8));
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmptySingleFront) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5, 6, 7, 8});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
}

TYPED_TEST(ForwardListTest, SpliceAfterIntoEmptySingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, otherList, jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
    EXPECT_THAT(otherList.empty(), true);
    EXPECT_THAT(collect(otherList), testing::ElementsAre());
}

TYPED_TEST(ForwardListTest, SpliceAfterMoveIntoEmptySingleOnly) {
    using List = TypeParam;
    using Value = typename List::value_type;
    List list;
    auto otherValues = create<Value>({5});
    List otherList(otherValues.begin(), otherValues.end());
    auto it = list.before_begin();
    auto jt = otherList.before_begin();
    list.splice_after(it, std::move(otherList), jt);
    EXPECT_THAT(list.empty(), false);
    EXPECT_THAT(collect(list), testing::ElementsAre(5));
}

// TODO: splice_after range.
// TODO: Consider generating tests instead.
