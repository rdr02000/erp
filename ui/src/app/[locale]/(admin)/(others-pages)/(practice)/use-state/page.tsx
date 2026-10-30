
import PageBreadcrumb from "@/components/common/PageBreadCrumb";
import ChatUI from "@/components/practice/ChatUI";
import EmployeeDetailsForm from "@/components/practice/ChatUI";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Employee Details | TailAdmin - Next.js Dashboard Template",
  description: "Enter employee personal and work information.",
};
export default function Practice() {
  return (
    <div>
      <PageBreadcrumb pageTitle="Employee Details" />
      <ChatUI />
    </div>
  );
}
